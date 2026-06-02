package legal_website.Services.verityEmail;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import legal_website.Dto.verityEmail.VerityEmailPayload;
import legal_website.Dto.verityEmail.VerityEmailResponse;
import legal_website.EntityAndRepo.Auth.UserEntity;
import legal_website.EntityAndRepo.Auth.UserRepo;
import legal_website.EntityAndRepo.outbox_events.OutboxEventEntity;
import legal_website.EntityAndRepo.outbox_events.OutboxEventsRepo;
import legal_website.EntityAndRepo.verification_codes.VerificationCodeEntity;
import legal_website.EntityAndRepo.verification_codes.VerificationCodesRepo;
import legal_website.Services.Jwt.JwtService;
import legal_website.common.errors.User.InactiveUserException;
import lombok.RequiredArgsConstructor;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
public class VerifyEmailService {
    private final UserRepo userRepo;
    private final VerificationCodesRepo verificationCodesRepo;
    private final PasswordEncoder encoder;
    private final OutboxEventsRepo outboxEventRepo;
    private final ObjectMapper objectMapper;
    private final JwtService jwtService;
    
    @Transactional
    public VerityEmailResponse sendCode(Long id){
        UserEntity user = userRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("нет пользователя"));
        if(user.isActive()==false){
            throw new InactiveUserException("не активный пользователь");
        }
        if(user.isEmailVerified()){
            throw new RuntimeException("email уже верифицирован");
        }
        // Логика отправки кода на email и создания записи в базе данных для проверки кода
        VerificationCodeEntity codeEntity = createVerificationCodeEntity(user);
        verificationCodesRepo.save(codeEntity);

        
        OutboxEventEntity outboxEvent = new OutboxEventEntity();
        outboxEvent.setAggregateType("USER");
        outboxEvent.setAggregateId(user.getId().toString());
        outboxEvent.setEventType("EMAIL_VERIFICATION_REQUESTED");
        outboxEvent.setCreatedAt(LocalDateTime.now());
        VerityEmailPayload kafkaPayload = createVerityEmailPayload(user, codeEntity);
        try {
            outboxEvent.setPayload(objectMapper.valueToTree(kafkaPayload));
        } catch (Exception e) {
            throw new IllegalStateException("Ошибка при сериализации данных для Kafka", e);
        }
        
        outboxEvent.setRetryCount(0);
        outboxEvent.setStatus("NEW");
        outboxEventRepo.save(outboxEvent);
        

        VerityEmailResponse response = makeVerityEmailResponse(user);

        return response;
    }
    private VerityEmailPayload createVerityEmailPayload(UserEntity user, VerificationCodeEntity codeEntity){
        VerityEmailPayload kafkaPayload = new VerityEmailPayload();
        kafkaPayload.setUserId(user.getId());
        kafkaPayload.setEmail(user.getEmail());
        kafkaPayload.setVerificationCodeId(codeEntity.getId());
        kafkaPayload.setPurpose(codeEntity.getPurpose());
        kafkaPayload.setChannel(codeEntity.getChannel());
        UUID eventId = UUID.randomUUID();
        kafkaPayload.setEventId(eventId);
        kafkaPayload.setLink(jwtService.generateLinkForVerifyEmail(kafkaPayload,codeEntity));
        return kafkaPayload;
    }
    private VerificationCodeEntity createVerificationCodeEntity(UserEntity user){
        VerificationCodeEntity codeEntity = new VerificationCodeEntity();
        codeEntity.setUserId(user);
        codeEntity.setChannel("email");
        codeEntity.setPurpose("VERIFY_EMAIL");
        codeEntity.setRecipient(user.getEmail());
        String verificationCode =generateVerificationCode(); 
        codeEntity.setCodeHash(encoder.encode(verificationCode));
        codeEntity.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        codeEntity.setCreatedAt(LocalDateTime.now());
        codeEntity.setUpdatedAt(LocalDateTime.now());
        codeEntity.setAttemptCount(0);
        codeEntity.setMaxAttempts(5);
        return codeEntity;
    }
    private VerityEmailResponse makeVerityEmailResponse(UserEntity user){
        VerityEmailResponse response = new VerityEmailResponse();
        response.setStatus("success");
        response.setChannel("email");
        response.setRecipientMasked(maskEmail(user.getEmail()));
        response.setExpiresInSeconds(300); // Код будет действителен 5 минут
        return response;
    }
    private String generateVerificationCode() {
        // Генерация случайного кода, например, 6-значного числового кода
        SecureRandom random = new SecureRandom();
    
        StringBuilder code = new StringBuilder();
    
        for (int i = 0; i < 6; i++) {
            code.append(random.nextInt(10));
        }
        
        return code.toString();
    }
    private String maskEmail(String email) {
        
        int atIndex = email.indexOf("@");
        if (atIndex <= 1) {
            return "****" + email.substring(atIndex);
        }
        String maskedPart = email.substring(0, atIndex - 1).replaceAll(".", "*");
        return maskedPart + email.substring(atIndex - 1);
    }
}
