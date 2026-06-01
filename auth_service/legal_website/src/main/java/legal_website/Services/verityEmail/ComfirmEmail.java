package legal_website.Services.verityEmail;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import legal_website.EntityAndRepo.Auth.UserEntity;
import legal_website.EntityAndRepo.Auth.UserRepo;
import legal_website.EntityAndRepo.verification_codes.VerificationCodeEntity;
import legal_website.EntityAndRepo.verification_codes.VerificationCodesRepo;
import legal_website.Services.Jwt.JwtService;

import legal_website.common.errors.User.InactiveUserException;
import legal_website.common.errors.User.UserNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ComfirmEmail {
    private final UserRepo userRepo;
    private final JwtService jwtService;
    private final VerificationCodesRepo codeRepo;
    
    @Transactional
    public boolean comfirmEmail(String token){
        Claims payload = jwtService.extractAllClaims(token);
        Long userId = Long.parseLong(payload.getSubject());
        VerificationCodeEntity codeEntity = codeRepo.findById(payload.get("verificationCodeId", Long.class))
            .orElseThrow(()-> new RuntimeException());
        if (!"VERIFY_EMAIL".equals(payload.get("purpose", String.class))) {
            throw new RuntimeException("неверный purpose у verification token");
        }
        if (!codeEntity.getCodeHash().equals(payload.get("codeHash", String.class))) {
            throw new RuntimeException("неверный verification token");
        }
        if (!codeEntity.getRecipient().equals(payload.get("email", String.class))) {
            throw new RuntimeException("email в verification token не совпадает");
        }
        if (!codeEntity.getUserId().getId().equals(userId)) {
            throw new RuntimeException("verification token не принадлежит пользователю");
        }
        if (codeEntity.getConsumedAt() != null) {
            throw new RuntimeException("verification token уже использован");
        }
        if (codeEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("verification token истёк");
        }
        UserEntity user = userRepo.findById(userId)
            .orElseThrow(()->new UserNotFoundException("нет пользователя"));
        if (!user.isActive()) {
            throw new InactiveUserException("не активный пользователь");
        }
        user.setEmailVerified(true);
        user.setUpdatedAt(LocalDateTime.now());
        codeEntity.setConsumedAt(LocalDateTime.now());
        codeEntity.setUpdatedAt(LocalDateTime.now());
        codeRepo.save(codeEntity);
        userRepo.save(user);
        return true; //пока boolean потом будет нормальный dto
    }
}
