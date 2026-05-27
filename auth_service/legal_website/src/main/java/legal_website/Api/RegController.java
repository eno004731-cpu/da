package legal_website.Api;


import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import legal_website.Dto.register.RegRequest;
import legal_website.Dto.register.AuthResponse;
import legal_website.Services.auth.register.RegService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class RegController {
    private final RegService regService;

    @PostMapping("/register")
    public AuthResponse regUser(@Valid @RequestBody RegRequest requestDto){
        return regService.regUser(requestDto);
    }
}
