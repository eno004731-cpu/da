package my_jira.users;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
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

                .build();
}
}