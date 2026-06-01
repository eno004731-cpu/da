package legal_website.Api;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import legal_website.Dto.verityEmail.ConfirmEmailRequest;
import legal_website.Dto.verityEmail.VerityEmailResponse;
import legal_website.Services.verityEmail.ComfirmEmail;
import legal_website.Services.verityEmail.VerityEmailService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/auth")
public class VerityEmailController {
    private final VerityEmailService verityEmailService;
    private final ComfirmEmail comfirmEmail;
    @PostMapping("") // я не знаю как называется endpoint
    public VerityEmailResponse verityEmail(Authentication authentication){
        Long userId =Long.parseLong(authentication.getName());
        return verityEmailService.sendCode(userId);
    }
    @PostMapping("")// я не знаю как называется endpoint
    public boolean confirmEmail(ConfirmEmailRequest request){
        return comfirmEmail.comfirmEmail(request.getToken());
    }

}
