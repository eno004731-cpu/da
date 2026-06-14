package legal_website.services.auth.login;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import legal_website.dto.logout.LogoutRequest;
import legal_website.persistence.jwt.JwtEntity;
import legal_website.persistence.jwt.JwtRepo;
import legal_website.services.jwt.JwtService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LogoutService {
    private final JwtService jwtService;
    private final JwtRepo jwtRepo;

    @Transactional
    public boolean logout(LogoutRequest request){
        JwtEntity jwt = jwtService.getValidRefreshToken(request.getRefreshToken());
        jwt.setRevokedAt(LocalDateTime.now());
        jwtRepo.save(jwt);
        return true;
    }
}
