package legal_website.Services.auth.login;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import legal_website.Dto.google.GoogleFillResponse;
import legal_website.Dto.google.GoogleFlowDto;
import legal_website.Dto.google.GoogleProfile;
import legal_website.Dto.google.GoogleRequest;
import legal_website.Dto.google.GoogleResponse;
import legal_website.Dto.register.AuthResponse;
import legal_website.Dto.register.AuthUserResponse;
import legal_website.EntityAndRepo.Auth.OAuthAccountEntity;
import legal_website.EntityAndRepo.Auth.OAuthAccountRepo;
import legal_website.EntityAndRepo.Auth.UserEntity;
import legal_website.Services.Jwt.JwtService;
import legal_website.Services.auth.AuthSessionService;
import legal_website.common.errors.OAuht.InvalidGoogleId;
import legal_website.common.errors.OAuht.OAuthConfigurationException;
import legal_website.common.errors.OAuht.OAuthProviderUnavailableException;
import legal_website.common.errors.User.InactiveUserException;
import legal_website.common.errors.token.TokenValidationException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GoogleLoginService {
    @Value("${google.client-id:}")
    private String googleClientId;
    private final OAuthAccountRepo oAuthAccountRepo;
    private final AuthSessionService authSessionService;
    private final JwtService jwtService;
    
    @Transactional
    public GoogleResponse loginGoogle(GoogleRequest request) {
        // 1. Проверяем подпись токена, audience и срок жизни.
        String credential = request.getCredential();
        GoogleIdToken.Payload payload = verifyAndReadPayload(credential);

        // 2. После verify payload уже можно безопасно читать.
        String googleSubject = payload.getSubject();
        String email = payload.getEmail();
        boolean emailVerified = Boolean.TRUE.equals(payload.getEmailVerified());
        String fullName = (String) payload.get("name");
        
 

        Optional<OAuthAccountEntity> userGoogle1 = oAuthAccountRepo.findByProviderAndProviderUserId("google", googleSubject);
        if(userGoogle1.isEmpty()){
            GoogleFlowDto flowDto = new GoogleFlowDto();
            flowDto.setSub(googleSubject);
            flowDto.setEmail(email);
            flowDto.setEmailVerified(emailVerified);
            flowDto.setFullName(fullName);
            flowDto.setProvider("google");  
            flowDto.setType("PROFILE_COMPLETION_REQUIRED");
            GoogleProfile profile = new GoogleProfile();
            
            profile.setFullName(fullName);
            GoogleFillResponse response = new GoogleFillResponse();
            response.setProfile(profile);
            response.setStatus("PROFILE_COMPLETION_REQUIRED");
            response.setFlowToken(jwtService.generateFlowToken(flowDto));
            GoogleResponse response2 = new GoogleResponse();
            response2.setGoogleResponse(response);
            return response2;
        }
        OAuthAccountEntity userGoogle = userGoogle1.get();
        userGoogle.setProviderEmail(email);
        userGoogle.setProviderEmailVerified(emailVerified);
        userGoogle.setLastLoginAt(LocalDateTime.now());
        oAuthAccountRepo.save(userGoogle);

        UserEntity user = userGoogle.getUser();
        if (!user.isActive()) {
            throw new InactiveUserException("не активный пользователь");
        }
        AuthUserResponse userResponse = authSessionService.buildUserResponse(user);
        String token = authSessionService.generateRefreshToken();
        authSessionService.saveToken(user, token);
        AuthResponse response = authSessionService.buildAuthResponse(user,token, userResponse);
        GoogleResponse response2 = new GoogleResponse();
        response2.setAuthResponse(response);
        return response2;    

    }
    
    
    private GoogleIdToken.Payload verifyAndReadPayload(String credential) {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new OAuthConfigurationException(
                "Не задан google.client-id. Добавь его в application.yaml, .env или переменные окружения."
            );
        }

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance()
            )
                .setAudience(Collections.singletonList(googleClientId))
                .build();

            GoogleIdToken idToken = verifier.verify(credential);
            if (idToken == null) {
                throw new InvalidGoogleId("Google ID token не прошёл verify.");
            }

            return idToken.getPayload();
        } catch (GeneralSecurityException | IOException exception) {
            throw new OAuthProviderUnavailableException("Не удалось провалидировать Google ID token.", exception);
        }
    }
}
