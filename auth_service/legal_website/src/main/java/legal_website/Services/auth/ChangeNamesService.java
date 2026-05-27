package legal_website.Services.auth;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import legal_website.Dto.MeRequest;
import legal_website.Dto.MeResponse;
import legal_website.EntityAndRepo.Auth.OAuthAccountEntity;
import legal_website.EntityAndRepo.Auth.OAuthAccountRepo;
import legal_website.EntityAndRepo.Auth.UserEntity;
import legal_website.EntityAndRepo.Auth.UserRepo;
import legal_website.common.errors.InactiveUserException;
import legal_website.common.errors.InvalidCredentialsException;
import legal_website.common.errors.UserNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChangeNamesService {
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final MeService meService;
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
