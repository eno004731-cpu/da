package my_jira.users.Me;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class UserMeController {

    private final UserMeService userMeService;

    public UserMeController(UserMeService userMeService){
        this.userMeService = userMeService;
    }

    @GetMapping("/me")
    public UserResponse getMe(HttpSession session,@Valid UserResponse response){

        Object userIdRaw = session.getAttribute("userId");
        if(userIdRaw == null){
            throw new RuntimeException("da");
        }
        Long id = (Long) userIdRaw;

        return userMeService.getMe(id, response);
    }
}
