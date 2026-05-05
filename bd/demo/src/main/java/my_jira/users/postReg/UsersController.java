package my_jira.users.postReg;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import my_jira.users.UsersRegisterDto;

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
    public boolean postUser(@Valid @RequestBody UsersRegisterDto request) {
        return authService.registerUser(request);
    }
}
