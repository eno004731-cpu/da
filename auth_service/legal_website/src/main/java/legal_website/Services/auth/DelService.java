package legal_website.Services.auth;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import legal_website.EntityAndRepo.Auth.OAuthAccountRepo;
import legal_website.EntityAndRepo.Auth.UserEntity;
import legal_website.EntityAndRepo.Auth.UserRepo;
import legal_website.EntityAndRepo.Jwt.JwtRepo;
import legal_website.common.errors.User.UserNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DelService {
    private final UserRepo userRepo;
    private final JwtRepo jwtRepo;
    private final OAuthAccountRepo oAuthAccountRepo;

    @Transactional
    public void delUser(Long userId){
        UserEntity user = userRepo.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("нет пользователя"));

        oAuthAccountRepo.deleteAllByUser(user);
        jwtRepo.deleteAllByUser(user);
        user.setActive(false);
        user.setEmail(null);
        user.setPhone(null);
        user.setUpdatedAt(LocalDateTime.now());
        userRepo.save(user);
    }
}
