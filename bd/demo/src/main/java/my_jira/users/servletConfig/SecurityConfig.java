package my_jira.users.servletConfig;

// Говорим Spring, что это конфигурационный класс
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Интерфейс encoder-а, который будем внедрять в сервис
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
// Реализация BCrypt для паролей
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import static jakarta.servlet.DispatcherType.ERROR;
import static jakarta.servlet.DispatcherType.FORWARD;

@Configuration
public class SecurityConfig {

    // Spring вызовет этот метод сам и зарегистрирует PasswordEncoder как bean
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Возвращаем реализацию BCrypt
        return new BCryptPasswordEncoder();
    }
    @Bean
    public  SecurityFilterChain web(HttpSecurity http){
        http
        
        .authorizeHttpRequests((authorize) -> authorize
            .dispatcherTypeMatchers(FORWARD, ERROR).permitAll()
            .requestMatchers("/api/auth/register").permitAll()
            .requestMatchers("/api/auth/login").permitAll()
            
            .anyRequest().authenticated())
            .httpBasic(AbstractHttpConfigurer::disable)

            
            .formLogin(AbstractHttpConfigurer::disable)
            .csrf((csrf) -> csrf

                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            );
            
        

   
    return http.build();
    }
}
