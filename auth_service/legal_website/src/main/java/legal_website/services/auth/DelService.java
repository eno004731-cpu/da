package legal_website.services.auth;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import legal_website.persistence.auth.OAuthAccountRepo;
import legal_website.persistence.auth.UserEntity;
import legal_website.persistence.auth.UserRepo;
import legal_website.persistence.jwt.JwtRepo;
import legal_website.common.errors.user.UserNotFoundException;
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
