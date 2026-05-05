package my_jira.users.delUser;

import org.springframework.stereotype.Service;


import lombok.RequiredArgsConstructor;
import my_jira.users.UsersEntity;
import my_jira.users.UsersRepo;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserDelService {
    private final UsersRepo usersRepo;

    
    public UsersEntity userDel(Long id){

        boolean haveuser = usersRepo.existsById(id);
        if (haveuser){
            Optional<UsersEntity> userOp = usersRepo.findById(id);
            UsersEntity user = userOp.get();
            user.setActive(false);
            user.setEmail(null);
            usersRepo.save(user);
            return user;
        }
        else{
            throw new RuntimeException("нет пользователя");
        }


    }
}
