package my_jira.auth.login;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import my_jira.auth.UsersEntity;
import my_jira.auth.UsersRepo;
@Service
@RequiredArgsConstructor
class UsersDetailService implements UserDetailsService {
    private final UsersRepo usersRepo ;

        @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        // Ищем пользователя в БД
        UsersEntity user = usersRepo.findByEmail(email)
        
        
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return  User.builder()
                .username(user.getEmail())

                // Здесь пароль уже должен быть зашифрован через BCrypt
                .password(user.getPasswordHash())

                // Например ROLE_USER
                .authorities(user.getRole())
                
                .disabled(!user.isActive())

                .build();
}
}