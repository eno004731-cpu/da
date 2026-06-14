package legal_website.services.auth;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import legal_website.dto.register.AuthResponse;
import legal_website.dto.register.AuthUserResponse;
import legal_website.persistence.auth.UserEntity;
import legal_website.persistence.jwt.JwtEntity;
import legal_website.persistence.jwt.JwtRepo;
import legal_website.services.jwt.JwtService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthSessionService {
    private final JwtRepo jwtRepo;
    private final JwtService jwtService;

    @Value("${jwt.refresh-days}")
    private long refreshDays;

    public String generateRefreshToken() {
        return jwtService.generateRefreshToken();
    }

    @Transactional
    public AuthUserResponse buildUserResponse(UserEntity user){
        AuthUserResponse userResponse = new AuthUserResponse();
        userResponse.setId(user.getId());
        userResponse.setFullName(user.getFullName());
        userResponse.setEmail(user.getEmail());
        userResponse.setPhone(user.getPhone());
        userResponse.setCompanyName(user.getCompanyName());
        userResponse.setRole(user.getRole());
        return userResponse;
    }

    @Transactional
    public void saveToken(UserEntity user,String token){
        JwtEntity jwt = new JwtEntity();
        jwt.setUser(user);
        jwt.setCreatedAt(LocalDateTime.now());
        jwt.setTokenHash(jwtService.hashToken(token));
        LocalDateTime exprires = LocalDateTime.now().plusDays(refreshDays);
        jwt.setExpiresAt(exprires);
        jwtRepo.save(jwt);
    }

    @Transactional
    public AuthResponse buildAuthResponse(UserEntity user,String refreshToken,AuthUserResponse userResponse){
        AuthResponse response = new AuthResponse();
        response.setAccessToken(jwtService.generateAccessToken(user));
        response.setRefreshToken(refreshToken);
        response.setTokenType("Bearer");
        response.setExpiresIn(jwtService.getAccessTokenExpiresInSeconds());
        response.setUser(userResponse);
        return response;
    }
    @Transactional
    public AuthResponse buildResponse(UserEntity user, String token){
        return buildAuthResponse(user, token, buildUserResponse(user));
    }
}
