package legal_website.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import legal_website.dto.google.GoogleFillRequest;
import legal_website.dto.google.GoogleRequest;
import legal_website.dto.google.GoogleResponse;
import legal_website.dto.login.LoginRequest;
import legal_website.dto.register.AuthResponse;
import legal_website.services.auth.login.GoogleFillService;
import legal_website.services.auth.login.GoogleLoginService;
import legal_website.services.auth.login.LoginService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class LoginController {
    private final LoginService loginService;
    private final GoogleLoginService googleLoginService;
    private final GoogleFillService googleFillService;
    @PostMapping("/login")
    public AuthResponse loginUser(@RequestBody @Valid LoginRequest request){
        return loginService.loginUser(request);
    }
    @PostMapping("/google/login")
    public GoogleResponse loginGoogle(@RequestBody @Valid GoogleRequest request){
        return googleLoginService.loginGoogle(request);
    }
    @PostMapping("/google/complete")
    public AuthResponse fillGoogleProfile(@RequestBody @Valid GoogleFillRequest request){
        return googleFillService.fillGoogle(request);
    }

}
