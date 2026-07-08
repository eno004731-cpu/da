package lawyer_service.configs;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import lawyer_service.repo_entity.LawyerEntity;
import lawyer_service.repo_entity.LawyerRepo;
import lawyer_service.repo_entity.enums.Role;
import lombok.RequiredArgsConstructor;
@Component
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
        private final LawyerRepo lawyerRepo;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        LawyerEntity lawyer = lawyerRepo.findByEmailAndIsActiveTrue(email)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));

        return User.builder()
        .username(lawyer.getEmail())
        .roles(Role.LAWYER.toString())
        .build();
        
    }
}
