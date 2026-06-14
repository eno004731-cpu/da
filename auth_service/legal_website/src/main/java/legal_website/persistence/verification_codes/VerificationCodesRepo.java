package legal_website.persistence.verification_codes;

import org.springframework.data.jpa.repository.JpaRepository;


public interface VerificationCodesRepo extends JpaRepository<VerificationCodeEntity, Long> {

    
}
