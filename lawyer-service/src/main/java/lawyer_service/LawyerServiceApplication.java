package lawyer_service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;

@Slf4j
@SpringBootApplication
@EnableCaching // Включает Spring Cache, который можно backed by Redis через application.yaml.
@EnableKafka // Разрешает Spring искать методы с @KafkaListener для обработки событий.
public class LawyerServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(LawyerServiceApplication.class, args);
		log.info("Lawyer Service application started");
	}

}
