package legal_website.api;

import org.springframework.validation.annotation.Validated;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import legal_website.dto.verifyemail.ConfirmEmailRequest;
import legal_website.dto.verifyemail.VerifyEmailResponse;
import legal_website.services.verifyemail.ConfirmEmail;
import legal_website.services.verifyemail.VerifyEmailService;
import lombok.RequiredArgsConstructor;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/auth/email-verification")
public class VerifyEmailController {
    private final VerifyEmailService verifyEmailService;
    private final ConfirmEmail confirmEmail;

    @PostMapping("/request")
    public VerifyEmailResponse verifyEmail(@AuthenticationPrincipal Long userId){
        return verifyEmailService.sendCode(userId);
    }

    @PostMapping("/confirm")
    public boolean confirmEmail(@Valid @RequestBody ConfirmEmailRequest request){
        return confirmEmail.confirmEmail(request.getToken());
    }

}
