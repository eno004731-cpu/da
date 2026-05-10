package my_jira.users.delUser;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@RestController
@RequestMapping("/api/auth")

public class UserDelController {
    private final UserDelService userDelService;

    public UserDelController(UserDelService userDelService){
        this.userDelService = userDelService;
    }


    @PostMapping("/account")
    public boolean delUser(Authentication authentication,
         HttpServletResponse response,
            HttpServletRequest request
    ){
        String email = authentication.getName();

        userDelService.userDel(email);

        new SecurityContextLogoutHandler()
                .logout(request, response, authentication);
        return true;
    }
}
