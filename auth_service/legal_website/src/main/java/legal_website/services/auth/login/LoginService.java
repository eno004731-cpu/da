package legal_website.services.auth.login;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import legal_website.dto.login.LoginRequest;
import legal_website.dto.register.AuthResponse;
import legal_website.dto.register.AuthUserResponse;
import legal_website.persistence.auth.UserEntity;
import legal_website.persistence.auth.UserRepo;
import legal_website.services.auth.AuthSessionService;
import legal_website.common.errors.user.InactiveUserException;
import legal_website.common.errors.user.UserNotFoundException;
import legal_website.common.errors.token.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginService {
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthSessionService authSessionService;
    
    public AuthResponse loginUser(LoginRequest request){
        UserEntity user = userRepo.findByEmail(request.getEmail())
            .orElseThrow(() -> new UserNotFoundException("нет пользователя"));
        if(!passwordEncoder.matches( request.getPassword(), user.getPasswordHash())){
            throw new InvalidCredentialsException("не правильный пароль");
        }
        if(user.isActive()==false){
            throw new InactiveUserException("не активный пользователь");
        }
        AuthUserResponse userResponse = authSessionService.buildUserResponse(user);
        
        String refreshToken = authSessionService.generateRefreshToken();
        authSessionService.saveToken(user, refreshToken);
        
        
        AuthResponse response = authSessionService.buildAuthResponse(user, refreshToken, userResponse);
        

        return response;
        
    }
}
