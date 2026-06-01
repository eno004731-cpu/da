package my_jira.auth.delete;

import org.springframework.stereotype.Service;


import lombok.RequiredArgsConstructor;
import my_jira.common.exception.UserNotFoundException;
import my_jira.auth.UsersEntity;
import my_jira.auth.UsersRepo;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserDelService {
    private final UsersRepo usersRepo;

    
    public UsersEntity userDel(String email){

        boolean haveuser = usersRepo.existsByEmail(email);
        if (haveuser){
            Optional<UsersEntity> userOp = usersRepo.findByEmail(email);
            UsersEntity user = userOp.get();
            user.setActive(false);
            user.setEmail(null);
            user.setPhone(null);
            usersRepo.save(user);
            return user;
        }
        else{
            throw new UserNotFoundException("Пользователь не найден");
        }


    }
}
