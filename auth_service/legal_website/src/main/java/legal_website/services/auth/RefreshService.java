package legal_website.services.auth;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import legal_website.dto.refreshtoken.TokenRequest;
import legal_website.dto.register.AuthResponse;
import legal_website.dto.register.AuthUserResponse;
import legal_website.persistence.auth.UserEntity;
import legal_website.persistence.jwt.JwtEntity;
import legal_website.persistence.jwt.JwtRepo;
import legal_website.services.jwt.JwtService;
import legal_website.common.errors.user.InactiveUserException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshService {
    private final JwtRepo jwtRepo;
    private final JwtService jwtService;
    private final AuthSessionService authSessionService;

    public AuthResponse refreshToken(TokenRequest request) {
        JwtEntity currentToken = jwtService.getValidRefreshToken(request.getRefreshToken());
        UserEntity user = currentToken.getUser();

        if (!user.isActive()) {
            throw new InactiveUserException("не активный пользователь");
        }

        currentToken.setRevokedAt(LocalDateTime.now());
        jwtRepo.save(currentToken);

        String newRefreshToken = authSessionService.generateRefreshToken();
        authSessionService.saveToken(user, newRefreshToken);
        

        AuthUserResponse userResponse = authSessionService.buildUserResponse(user);
        AuthResponse response = authSessionService.buildAuthResponse(user, newRefreshToken, userResponse);


        return response;
    }
}
