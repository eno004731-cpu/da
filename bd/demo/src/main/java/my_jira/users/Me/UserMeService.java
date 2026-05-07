package my_jira.users.Me;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.util.Optional;

import my_jira.users.UsersEntity;
import my_jira.users.UsersRepo;

@Service
@RequiredArgsConstructor
public class UserMeService {
    final private UsersRepo usersRepo;
    public UserResponse getMe(Long id, UserResponse response){
        Optional<UsersEntity> userOp = usersRepo.findById(id);
        if (userOp.isPresent()){
            UsersEntity user = userOp.get();
            response.setCompanyName(user.getCompanyName());
            response.setEmail(user.getEmail());
            response.setFullName(user.getFullName());
            response.setPhone(user.getPhone());
            response.setId(user.getId());
            response.setRole(user.getRole());
            return response;
            
        } else {
            throw new RuntimeException("нет пользователя");
        }
    }
}
