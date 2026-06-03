package legal_website.Api;

import org.springframework.validation.annotation.Validated;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import legal_website.Dto.verityEmail.ConfirmEmailRequest;
import legal_website.Dto.verityEmail.VerityEmailResponse;
import legal_website.Services.verityEmail.ComfirmEmail;
import legal_website.Services.verityEmail.VerifyEmailService;
import lombok.RequiredArgsConstructor;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/auth/email-verification")
public class VerityEmailController {
    private final VerifyEmailService verityEmailService;
    private final ComfirmEmail comfirmEmail;

    @PostMapping("/request")
    public VerityEmailResponse verityEmail(@AuthenticationPrincipal Long userId){
        return verityEmailService.sendCode(userId);
    }

    @PostMapping("/confirm")
    public boolean confirmEmail(@Valid @RequestBody ConfirmEmailRequest request){
        return comfirmEmail.comfirmEmail(request.getToken());
    }

}
