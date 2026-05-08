package my_jira.users.postReg;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import my_jira.users.UsersEntity;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UsersController {
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;


       
    // Базовый endpoint регистрации клиента:
    // frontend отправляет JSON в POST /api/auth/register
    @PostMapping("/register")
    public boolean postUser(@Valid @RequestBody UsersRegisterDto requestDto,
                                                HttpSession session,
                                                HttpServletRequest request,
                                                HttpServletResponse response) {
        
    authService.registerUser(requestDto);

                                                


    UsernamePasswordAuthenticationToken token =
            UsernamePasswordAuthenticationToken.unauthenticated(
                    requestDto.getEmail(),
                    requestDto.getPassword()
            );

    // 3. Проверяем пользователя через Spring Security
    // AuthenticationManager вернёт уже авторизованный Authentication
    Authentication authentication =
            authenticationManager.authenticate(token);

    // 4. Создаём SecurityContext
    SecurityContext context =
            SecurityContextHolder.createEmptyContext();

    // 5. Кладём туда авторизованного пользователя
    context.setAuthentication(authentication);

    // 6. Устанавливаем context для текущего запроса
    SecurityContextHolder.setContext(context);

    // 7. Сохраняем context в session
    // Без этого пользователь может залогиниться только на текущий запрос
    securityContextRepository.saveContext(context, request, response);

      return true;
    }
}
