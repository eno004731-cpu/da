package my_jira.users.postReg;

import java.time.LocalDateTime;




import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.RequiredArgsConstructor;
import my_jira.users.UsersEntity;
import my_jira.users.UsersRepo;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UsersRepo usersRepo;
    private final PasswordEncoder passwordEncoder;
    

    
    public UsersEntity registerUser (UsersRegisterDto request){
        String email = request.getEmail();
    
        boolean haveEmail =usersRepo.existsByEmail(email);
    if (haveEmail){
        throw new RuntimeException("проблема с email");
    }
    else{
        String pass= request.getPassword();
        String passwordHash= passwordEncoder.encode(pass);
        UsersEntity user = new UsersEntity();
        
        user.setRole("CLIENT");
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setCompanyName(request.getCompanyName());
        user.setPasswordHash(passwordHash);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        usersRepo.save(user);
        return user;
    }
    
} 
}
