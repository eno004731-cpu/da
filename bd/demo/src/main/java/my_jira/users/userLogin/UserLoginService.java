package my_jira.users.userLogin;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import my_jira.users.UsersEntity;
import my_jira.users.UsersRepo;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserLoginService {
    private final UsersRepo usersRepo;

    private final PasswordEncoder passwordEncoder;


    public UsersEntity loginUser(UserLoginDTO request){

        Optional<UsersEntity> userOp =usersRepo.findByEmail(request.getEmail());
        if(userOp.isEmpty()){
            throw new RuntimeException("Пользователь не найден");
        }
        
        UsersEntity user = userOp.get();
        String pass = request.getPassword();
        String passH = user.getPasswordHash();
        boolean match = passwordEncoder.matches(pass,passH);
        if (!user.isActive()) {
            throw new RuntimeException("Аккаунт деактивирован");
        }
        if (!match){
            throw new RuntimeException("Неверный пароль");
        }
        
        return user;
    }
}
