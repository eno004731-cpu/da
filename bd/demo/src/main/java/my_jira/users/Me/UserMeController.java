package my_jira.users.Me;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    public UserResponse getMe(Authentication authentication){
        String email = authentication.getName();
        
        if(email == null){
            throw new RuntimeException("нет такго email");
        }
        

        return userMeService.getMe(email);
    }
}
