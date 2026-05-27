package legal_website.Services.auth.login;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import legal_website.Dto.login.LoginRequest;
import legal_website.Dto.register.AuthResponse;
import legal_website.Dto.register.AuthUserResponse;
import legal_website.EntityAndRepo.Auth.UserEntity;
import legal_website.EntityAndRepo.Auth.UserRepo;
import legal_website.Services.auth.AuthSessionService;
import legal_website.common.errors.InactiveUserException;
import legal_website.common.errors.InvalidCredentialsException;
import legal_website.common.errors.UserNotFoundException;
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
