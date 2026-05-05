package my_jira.users.delUser;



import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;




@RestController
@RequestMapping("/api/auth")
public class UserDelController {
    private final UserDelService userDelService;

    public UserDelController(UserDelService userDelService){
        this.userDelService = userDelService;
    }


    @PostMapping("/account")
    public void delUser(HttpSession session){

        Object userIdRaw = session.getAttribute("userId");
        if(userIdRaw == null){
            throw new RuntimeException("da");
        }
        Long id = (Long) userIdRaw;

        userDelService.userDel(id);
        
        session.invalidate();

    }
}
