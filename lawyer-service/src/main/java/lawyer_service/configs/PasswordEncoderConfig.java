package lawyer_service.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordEncoderConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		// Strength 12 задает стоимость BCrypt: чем выше число, тем медленнее подбор пароля.
		// Для auth-сервиса это хороший баланс между безопасностью и скоростью на старте проекта.
		return new BCryptPasswordEncoder(12);
	}

}
