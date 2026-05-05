package my_jira.users.userLogin;


import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import my_jira.users.UsersEntity;

@RestController
@RequestMapping("/api/auth")
public class UserLoginController {
    private final UserLoginService userLoginService;


    public UserLoginController(UserLoginService userLoginService){
        this.userLoginService=userLoginService;
    }

    @PostMapping("/login")
    public boolean postUser(@Valid @RequestBody UserLoginDTO request,HttpSession session){

    UsersEntity user = userLoginService.loginUser(request);


    session.setAttribute("userId", user.getId());

    session.setAttribute("userRole", user.getRole());

    // 4. Возвращаем успешный ответ
    return true;

    }}    

