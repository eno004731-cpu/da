package my_jira.users.postReg;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import my_jira.users.UsersEntity;

@RestController
@RequestMapping("/api/auth")
public class UsersController {
    private final AuthService authService;

    public UsersController(AuthService authService) {
        this.authService = authService;
    }

    // Базовый endpoint регистрации клиента:
    // frontend отправляет JSON в POST /api/auth/register
    @PostMapping("/register")
    public boolean postUser(@Valid @RequestBody UsersRegisterDto request,HttpSession session) {
        
    UsersEntity user = authService.registerUser(request);


    session.setAttribute("userId", user.getId());

    session.setAttribute("userRole", user.getRole());

      return true;
    }
}
