package legal_website.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import legal_website.dto.logout.LogoutRequest;
import legal_website.services.auth.login.LogoutService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class LogoutController {
    private final LogoutService logoutService;
    @PostMapping("/logout")
    public boolean logout(@RequestBody @Valid LogoutRequest request){
        return logoutService.logout(request);
    }
}
