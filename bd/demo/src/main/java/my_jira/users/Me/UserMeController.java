package my_jira.users.Me;

import my_jira.common.exception.AuthenticationRequiredException;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/auth")
public class UserMeController {

    private final UserMeService userMeService;

    public UserMeController(UserMeService userMeService){
        this.userMeService = userMeService;
    }

    @GetMapping("/me")
    public UserResponse getMe(Authentication authentication){
        if (authentication == null) {
            throw new AuthenticationRequiredException("Пользователь не аутентифицирован");
        }

        String email = authentication.getName();
        
        if(email == null){
            throw new AuthenticationRequiredException("Не удалось определить email текущего пользователя");
        }
        

        return userMeService.getMe(email);
    }
}
