package legal_website.Services.auth.login;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.jsonwebtoken.Claims;
import legal_website.Dto.google.GoogleFillRequest;
import legal_website.Dto.register.AuthResponse;
import legal_website.EntityAndRepo.Auth.OAuthAccountEntity;
import legal_website.EntityAndRepo.Auth.OAuthAccountRepo;
import legal_website.EntityAndRepo.Auth.UserEntity;
import legal_website.EntityAndRepo.Auth.UserRepo;
import legal_website.Services.Jwt.JwtService;
import legal_website.Services.auth.AuthSessionService;
import legal_website.common.errors.OAuht.OAuthAccountAlreadyLinkedException;
import legal_website.common.errors.token.InvalidFlowTokenException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GoogleFillService {
    private final JwtService jwtService;
    private final AuthSessionService authSessionService;
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final OAuthAccountRepo oAuthAccountRepo;
    
    @Transactional
    public AuthResponse fillGoogle(GoogleFillRequest request){
        
        String rawFlowToken = request.getFlowToken();
        Claims claims;
        try {
            claims = jwtService.extractAllClaims(rawFlowToken);
        } catch (RuntimeException exception) {
            throw new InvalidFlowTokenException("Invalid flow token");
        }
        
        if (!"PROFILE_COMPLETION_REQUIRED".equals(claims.get("type", String.class))) {
            throw new InvalidFlowTokenException("Invalid flow token type");
        }
        if (!"google".equals(claims.get("provider", String.class))) {
            throw new InvalidFlowTokenException("Invalid flow token provider");
        }
        String providerUserId = claims.get("sub", String.class);
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new InvalidFlowTokenException("Provider user id is required in flow token");
        }
        String email = claims.get("email", String.class);
        if (email == null || email.isBlank()) {
            throw new InvalidFlowTokenException("Email claim is required in flow token");
        }
        UserEntity existingUser = userRepo.findByEmail(email).orElse(null);
        if (existingUser != null && existingUser.isActive()) {
             String refreshToken = authSessionService.generateRefreshToken();
            if(oAuthAccountRepo.existsByProviderAndProviderUserId("google", providerUserId)){
                authSessionService.saveToken(existingUser, refreshToken);
                return authSessionService.buildAuthResponse(existingUser, refreshToken, authSessionService.buildUserResponse(existingUser));

            }
            OAuthAccountEntity oAuthAccount = new OAuthAccountEntity();
            oAuthAccount.setProviderUserId(providerUserId);
            oAuthAccount.setProvider(claims.get("provider", String.class));
            oAuthAccount.setUser(existingUser);
            oAuthAccount.setCreatedAt(LocalDateTime.now());
            oAuthAccount.setUpdatedAt(LocalDateTime.now());
            oAuthAccount.setProviderEmail(email);
            oAuthAccount.setProviderEmailVerified(claims.get("emailVerified", Boolean.class));
            oAuthAccountRepo.save(oAuthAccount);
           
            authSessionService.saveToken(existingUser, refreshToken);
            return authSessionService.buildAuthResponse(existingUser, refreshToken, authSessionService.buildUserResponse(existingUser));
        }

        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setFullName(request.getFullName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole("CLIENT");
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepo.save(user);
        OAuthAccountEntity oAuthAccount = new OAuthAccountEntity();
        oAuthAccount.setProviderUserId(providerUserId);
        oAuthAccount.setProvider(claims.get("provider", String.class));
        oAuthAccount.setUser(user);
        oAuthAccount.setCreatedAt(LocalDateTime.now());
        oAuthAccount.setProviderEmail(email);
        oAuthAccount.setUpdatedAt(LocalDateTime.now());
        oAuthAccount.setProviderEmailVerified(claims.get("emailVerified", Boolean.class));
        oAuthAccountRepo.save(oAuthAccount);
       String refreshToken = authSessionService.generateRefreshToken();
        authSessionService.saveToken(user, refreshToken);
        return authSessionService.buildAuthResponse(user, refreshToken, authSessionService.buildUserResponse(user));
    }
}
