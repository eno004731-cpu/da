package my_jira.servletConfig;

// Говорим Spring, что это конфигурационный класс
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Интерфейс encoder-а, который будем внедрять в сервис
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
// Реализация BCrypt для паролей
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import static jakarta.servlet.DispatcherType.ERROR;
import static jakarta.servlet.DispatcherType.FORWARD;

import java.time.LocalDateTime;

@Configuration
public class SecurityConfig {

    // Spring вызовет этот метод сам и зарегистрирует PasswordEncoder как bean
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Возвращаем реализацию BCrypt
        return new BCryptPasswordEncoder();
    }
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        // Следит за созданием и удалением HTTP-сессий
        return new HttpSessionEventPublisher();
    }
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        // Достаём AuthenticationManager из Spring Security
        // Он будет проверять username/password
        return configuration.getAuthenticationManager();
    }
    @Bean
    public SecurityContextRepository da(){
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public  SecurityFilterChain web(HttpSecurity http){
        http
        
        .authorizeHttpRequests((authorize) -> authorize
            .dispatcherTypeMatchers(FORWARD, ERROR).permitAll()
            .requestMatchers("/api/auth/csrf").permitAll()
            .requestMatchers("/api/auth/register").permitAll()
            .requestMatchers("/api/auth/login").permitAll()
            
            .anyRequest().authenticated())
            .httpBasic(AbstractHttpConfigurer::disable)

            
            .formLogin(AbstractHttpConfigurer::disable)
            .csrf((csrf) -> csrf

                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
    )
                .sessionManagement((session) -> session
                // Разрешаем только одну активную сессию на пользователя
                .maximumSessions(1)
            )
            .logout((logout) -> logout
            // URL для выхода
            .logoutUrl("/api/auth/logout")

            // Удаляем cookie сессии
            .deleteCookies("JSESSIONID")

            // Разрешаем logout всем
            .permitAll()
        );


    return http.build();
    }

    
}
