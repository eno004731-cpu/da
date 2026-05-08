package my_jira.users.userLogin;


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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserLoginController {
//     private final UserLoginService userLoginService;
        private final AuthenticationManager authenticationManager;
        private final SecurityContextRepository securityContextRepository;

 


        
    @PostMapping("/login")
    public boolean postUser(@Valid @RequestBody UserLoginDTO requestDto,
                           
                            HttpServletRequest request,
                            HttpServletResponse response){




    
         UsernamePasswordAuthenticationToken token =
            UsernamePasswordAuthenticationToken.unauthenticated(
                    requestDto.getEmail(),
                    requestDto.getPassword()
            );
    // Создаём неавторизованный token из username и password

    Authentication authentication =
            authenticationManager.authenticate(token);
    // Проверяем пользователя через AuthenticationManager

    SecurityContext context =
            SecurityContextHolder.createEmptyContext();
    // Создаём пустой SecurityContext

    context.setAuthentication(authentication);
    // Кладём авторизованного пользователя в SecurityContext
    SecurityContextHolder.setContext(context);
    // Устанавливаем SecurityContext в текущий поток

    securityContextRepository.saveContext(context, request, response);
    // Сохраняем SecurityContext в сессию

    // 4. Возвращаем успешный ответ
    return true;

    }}    

