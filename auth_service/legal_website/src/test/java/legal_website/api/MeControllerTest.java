package legal_website.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import legal_website.dto.MeResponse;
import legal_website.services.auth.ChangeNamesService;
import legal_website.services.auth.MeService;

class MeControllerTest {

    @Test
    void getMeShouldReturnCurrentUserFromAuthenticationName() {
        MeService meService = Mockito.mock(MeService.class);
        ChangeNamesService changeNamesService = Mockito.mock(ChangeNamesService.class);

        MeResponse response = new MeResponse();
        response.setId(7L);
        response.setEmail("ivan@test.ru");

        when(meService.getMe(7L)).thenReturn(response);

        MeController controller = new MeController(meService, changeNamesService);
        MeResponse result = controller.getMe(7L);

        assertEquals(7L, result.getId());
        assertEquals("ivan@test.ru", result.getEmail());
        verify(meService).getMe(7L);
    }
}
