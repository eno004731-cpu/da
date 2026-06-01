package legal_website.Services.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import legal_website.Dto.MeResponse;
import legal_website.EntityAndRepo.Auth.OAuthAccountRepo;
import legal_website.EntityAndRepo.Auth.UserEntity;
import legal_website.EntityAndRepo.Auth.UserRepo;
import legal_website.common.errors.User.InactiveUserException;
import legal_website.common.errors.User.UserNotFoundException;

@ExtendWith(MockitoExtension.class)
class MeServiceTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private OAuthAccountRepo oAuthAccountRepo;

    @InjectMocks
    private MeService meService;

    @Test
    void getMeShouldReturnCurrentUser() {
        UserEntity user = new UserEntity();
        user.setId(5L);
        user.setEmail("ivan@test.ru");
        user.setFullName("Ivan Ivanov");
        user.setPhone("+79990000000");
        user.setCompanyName("OOO Test");
        user.setRole("CLIENT");
        user.setActive(true);

        when(userRepo.findById(5L)).thenReturn(Optional.of(user));
        when(oAuthAccountRepo.findAllByUser(user)).thenReturn(java.util.List.of());

        MeResponse response = meService.getMe(5L);

        assertEquals(5L, response.getId());
        assertEquals("ivan@test.ru", response.getEmail());
        assertEquals("Ivan Ivanov", response.getFullName());
    }

    @Test
    void getMeShouldThrowWhenUserNotFound() {
        when(userRepo.findById(99L)).thenReturn(Optional.empty());

        UserNotFoundException error = assertThrows(
                UserNotFoundException.class,
                () -> meService.getMe(99L)
        );

        assertEquals("нет пользователя", error.getMessage());
    }

    @Test
    void getMeShouldThrowWhenUserInactive() {
        UserEntity user = new UserEntity();
        user.setId(10L);
        user.setActive(false);

        when(userRepo.findById(10L)).thenReturn(Optional.of(user));

        InactiveUserException error = assertThrows(
                InactiveUserException.class,
                () -> meService.getMe(10L)
        );

        assertEquals("не активный пользователь", error.getMessage());
    }
}
