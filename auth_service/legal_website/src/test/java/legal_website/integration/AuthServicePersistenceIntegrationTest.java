package legal_website.integration;

import legal_website.dto.register.AuthResponse;
import legal_website.dto.register.RegRequest;
import legal_website.dto.verifyemail.VerifyEmailResponse;
import legal_website.persistence.auth.UserEntity;
import legal_website.persistence.jwt.JwtEntity;
import legal_website.persistence.outbox_events.OutboxEventEntity;
import legal_website.persistence.verification_codes.VerificationCodeEntity;
import legal_website.services.jwt.JwtService;
import legal_website.services.auth.register.RegService;
import legal_website.services.verifyemail.VerifyEmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthServicePersistenceIntegrationTest extends PostgresAuthIntegrationTestBase {

    @Autowired
    RegService regService;

    @Autowired
    VerifyEmailService verifyEmailService;

    @Autowired
    JwtService jwtService;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void registrationStoresUserAndRefreshTokenInPostgres() {
        RegRequest request = registrationRequest("ivan-" + UUID.randomUUID() + "@test.ru");

        AuthResponse response = regService.regUser(request);

        UserEntity user = userRepo.findByEmail(request.getEmail()).orElseThrow();
        assertThat(user.getRole()).isEqualTo("CLIENT");
        assertThat(user.getFullName()).isEqualTo("Ivan Ivanov");
        assertThat(user.isActive()).isTrue();
        assertThat(user.isEmailVerified()).isFalse();
        assertThat(user.getPasswordHash()).isNotEqualTo(request.getPassword());
        assertThat(passwordEncoder.matches(request.getPassword(), user.getPasswordHash())).isTrue();

        List<JwtEntity> refreshTokens = jwtRepo.findAllByUser(user);
        assertThat(refreshTokens).hasSize(1);
        assertThat(refreshTokens.get(0).getTokenHash()).isEqualTo(jwtService.hashToken(response.getRefreshToken()));
        assertThat(refreshTokens.get(0).getRevokedAt()).isNull();

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void sendEmailVerificationCodeStoresCodeAndOutboxEventInPostgres() {
        RegRequest request = registrationRequest("verify-" + UUID.randomUUID() + "@test.ru");
        regService.regUser(request);
        UserEntity user = userRepo.findByEmail(request.getEmail()).orElseThrow();

        VerifyEmailResponse response = verifyEmailService.sendCode(user.getId());

        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getChannel()).isEqualTo("email");
        assertThat(response.getExpiresInSeconds()).isEqualTo(300);

        assertThat(verificationCodesRepo.findAll()).hasSize(1);
        VerificationCodeEntity code = verificationCodesRepo.findAll().get(0);
        assertThat(code.getUserId().getId()).isEqualTo(user.getId());
        assertThat(code.getChannel()).isEqualTo("email");
        assertThat(code.getPurpose()).isEqualTo("VERIFY_EMAIL");
        assertThat(code.getRecipient()).isEqualTo(user.getEmail());
        assertThat(code.getConsumedAt()).isNull();
        assertThat(code.getAttemptCount()).isZero();
        assertThat(code.getMaxAttempts()).isEqualTo(5);

        assertThat(outboxEventsRepo.findAll()).hasSize(1);
        OutboxEventEntity outboxEvent = outboxEventsRepo.findAll().get(0);
        assertThat(outboxEvent.getAggregateType()).isEqualTo("USER");
        assertThat(outboxEvent.getAggregateId()).isEqualTo(user.getId().toString());
        assertThat(outboxEvent.getEventType()).isEqualTo("EMAIL_VERIFICATION_REQUESTED");
        assertThat(outboxEvent.getStatus()).isEqualTo("NEW");
        assertThat(outboxEvent.getRetryCount()).isZero();
        assertThat(outboxEvent.getPayload().get("userId").asLong()).isEqualTo(user.getId());
        assertThat(outboxEvent.getPayload().get("email").asText()).isEqualTo(user.getEmail());
        assertThat(outboxEvent.getPayload().get("verificationCodeId").asLong()).isEqualTo(code.getId());
        assertThat(outboxEvent.getPayload().get("purpose").asText()).isEqualTo("VERIFY_EMAIL");
        assertThat(outboxEvent.getPayload().get("link").asText()).startsWith("http://127.0.0.1:8000/verify-email?token=");
    }

    private RegRequest registrationRequest(String email) {
        RegRequest request = new RegRequest();
        request.setFullName("Ivan Ivanov");
        request.setEmail(email);
        request.setPhone("+7999" + Math.abs(email.hashCode() % 1_000_0000));
        request.setCompanyName("OOO Test");
        request.setPassword("Password1!");
        return request;
    }
}
