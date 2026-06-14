package legal_website.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import legal_website.dto.refreshtoken.TokenRequest;
import legal_website.dto.register.AuthResponse;
import legal_website.services.auth.RefreshService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class RefreshController {
    private final RefreshService refreshService;
    @PostMapping("/refresh")
    public AuthResponse refreshToken(@RequestBody @Valid TokenRequest request){
        return refreshService.refreshToken(request);
    }
}
