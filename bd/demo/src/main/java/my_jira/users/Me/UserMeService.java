package my_jira.users.Me;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.util.Optional;

import my_jira.common.exception.UserNotFoundException;
import my_jira.users.UsersEntity;
import my_jira.users.UsersRepo;

@Service
@RequiredArgsConstructor
public class UserMeService {
    final private UsersRepo usersRepo;
    public UserResponse getMe(String email){
        Optional<UsersEntity> userOp = usersRepo.findByEmail(email);
        if (userOp.isPresent()){
            UsersEntity user = userOp.get();
            UserResponse response = new UserResponse();
            response.setCompanyName(user.getCompanyName());
            response.setEmail(user.getEmail());
            response.setFullName(user.getFullName());
            response.setPhone(user.getPhone());
            response.setId(user.getId());
            response.setRole(user.getRole());
            return response;

        } else {
            throw new UserNotFoundException("Пользователь не найден");
        }
    }
}
