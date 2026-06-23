package legal_website.services.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import legal_website.persistence.deletion.UserDeletionProcessRepo;
import legal_website.persistence.outbox_events.OutboxEventsRepo;
import legal_website.services.delete.DelService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import legal_website.persistence.auth.OAuthAccountRepo;
import legal_website.persistence.auth.UserEntity;
import legal_website.persistence.auth.UserRepo;
import legal_website.persistence.jwt.JwtRepo;

@ExtendWith(MockitoExtension.class)
class DelServiceTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private JwtRepo jwtRepo;

    @Mock
    private OAuthAccountRepo oAuthAccountRepo;

    @Mock
    private UserDeletionProcessRepo userDeletionProcessRepo;

    @Mock
    private OutboxEventsRepo outboxEventsRepo;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private DelService delService;

    @Test
    void delUserShouldSoftDeleteUserAndRemoveTokensAndOauthLinks() {
        UserEntity user = new UserEntity();
        ReflectionTestUtils.setField(user, "id", 17L);
        user.setEmail("ivan@test.ru");
        user.setPhone("+79990000000");
        user.setFullName("Ivan Ivanov");
        user.setRole("CLIENT");
        user.setPasswordHash("hashed-password");
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now().minusDays(30));
        user.setUpdatedAt(LocalDateTime.now().minusDays(1));

        when(userRepo.findById(17L)).thenReturn(Optional.of(user));

        delService.delUser(17L);

        verify(oAuthAccountRepo).deleteAllByUser(user);
        verify(jwtRepo).deleteAllByUser(user);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepo).save(userCaptor.capture());

        UserEntity savedUser = userCaptor.getValue();
        assertFalse(savedUser.isActive());
        assertNull(savedUser.getEmail());
        assertNull(savedUser.getPhone());
        assertEquals("Ivan Ivanov", savedUser.getFullName());
        assertEquals("CLIENT", savedUser.getRole());
        assertEquals("hashed-password", savedUser.getPasswordHash());
    }
}
