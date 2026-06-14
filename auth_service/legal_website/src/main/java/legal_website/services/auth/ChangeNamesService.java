package legal_website.services.auth;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import legal_website.dto.MeRequest;
import legal_website.dto.MeResponse;
import legal_website.persistence.auth.OAuthAccountEntity;
import legal_website.persistence.auth.OAuthAccountRepo;
import legal_website.persistence.auth.UserEntity;
import legal_website.persistence.auth.UserRepo;
import legal_website.common.errors.user.InactiveUserException;
import legal_website.common.errors.user.UserNotFoundException;
import legal_website.common.errors.token.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChangeNamesService {
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final MeService meService;
    @Transactional
    public MeResponse changeNames(MeRequest request, Long userId){
        UserEntity user = userRepo.findById(userId).orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));
            if (!user.isActive()) {
            throw new InactiveUserException("не активный пользователь");
        }
        user.setFullName(request.getFullName());
        user.setCompanyName(request.getCompanyName());
        boolean isPasswordCorrect = passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash());
        if (!isPasswordCorrect) {
            throw new InvalidCredentialsException("Неверный текущий пароль");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepo.save(user);
        return meService.MakeMeResponse(user);
                
    }
}
