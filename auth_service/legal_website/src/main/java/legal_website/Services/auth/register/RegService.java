package legal_website.Services.auth.register;

import java.time.LocalDateTime;


import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import legal_website.Dto.register.AuthResponse;
import legal_website.Dto.register.AuthUserResponse;
import legal_website.Dto.register.RegRequest;
import legal_website.EntityAndRepo.Auth.UserEntity;
import legal_website.EntityAndRepo.Auth.UserRepo;
import legal_website.Services.auth.AuthSessionService;
import legal_website.common.errors.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RegService {
    private final PasswordEncoder encoder;
    private final UserRepo authRepo;
    private final AuthSessionService authSessionService;

    public AuthResponse regUser(RegRequest request){
        
        if(authRepo.existsByEmail(request.getEmail())==true){
            throw new UserAlreadyExistsException("уже есть такой пользователь");
        }
        if(request.getPhone() != null && authRepo.existsByPhone(request.getPhone())){
            throw new UserAlreadyExistsException("уже существует пользователь с таким номером");
        }
        UserEntity user = new UserEntity();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setCompanyName(request.getCompanyName());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setPasswordHash(hashPassword(request.getPassword()));
        user.setRole("CLIENT");
        user.setActive(true);
        
        authRepo.save(user);

        String refreshToken = authSessionService.generateRefreshToken();
        authSessionService.saveToken(user, refreshToken);

        AuthUserResponse userResponse = authSessionService.buildUserResponse(user);

        AuthResponse response = authSessionService.buildAuthResponse(user, refreshToken, userResponse);

        return response;
    }
    private String hashPassword(String password){
        return encoder.encode(password);
    }
}
