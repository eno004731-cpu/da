package legal_website.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import legal_website.dto.MeRequest;
import legal_website.dto.MeResponse;
import legal_website.services.auth.ChangeNamesService;
import legal_website.services.auth.MeService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class MeController {
    private final MeService meService;
    private final ChangeNamesService changeNamesService;
    @GetMapping("/me")
    public MeResponse getMe(@AuthenticationPrincipal Long userId){
        return meService.getMe(userId);
    }
    @PatchMapping("/me")
    public MeResponse changeNames(@RequestBody MeRequest request, @AuthenticationPrincipal Long userId){
        return changeNamesService.changeNames(request, userId); 
    }
}
