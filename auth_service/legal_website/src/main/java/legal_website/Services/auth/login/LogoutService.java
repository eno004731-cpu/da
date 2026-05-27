package legal_website.Services.auth.login;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import legal_website.Dto.logout.LogoutRequest;
import legal_website.EntityAndRepo.Jwt.JwtEntity;
import legal_website.EntityAndRepo.Jwt.JwtRepo;
import legal_website.Services.Jwt.JwtService;
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
