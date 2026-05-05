package my_jira.users.postReg;

// Говорим Spring, что это конфигурационный класс
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Интерфейс encoder-а, который будем внедрять в сервис
import org.springframework.security.crypto.password.PasswordEncoder;

// Реализация BCrypt для паролей
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class SecurityConfig {

    // Spring вызовет этот метод сам и зарегистрирует PasswordEncoder как bean
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Возвращаем реализацию BCrypt
        return new BCryptPasswordEncoder();
    }
}
