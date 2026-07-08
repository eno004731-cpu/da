package lawyer_service.api;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lawyer_service.dto.AuthRequest;
import lawyer_service.dto.AuthResponse;
import lawyer_service.services.RegisterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("api/auth/lawyer")
public class LawyerApi {
    private final RegisterService registerService;
    @PostMapping("register")
    public ResponseEntity<AuthResponse> reg(
        @RequestBody AuthRequest authRequest
    ){
        AuthResponse authResponse = registerService.reg(authRequest);
        ResponseCookie responseCookie = ResponseCookie.from("refreshToken",authResponse.getRefreshToken())
            .httpOnly(true)
            .secure(false)
            .sameSite("Strict")
            .path("/api/auth")
            .maxAge(Duration.ofDays(15))
            .build();
        authResponse.setRefreshToken(null);
        return ResponseEntity.status(HttpStatus.CREATED)
            .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
            .body(authResponse);
    }
}
