package legal_website.common;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import legal_website.common.errors.AccessDeniedException;
import legal_website.common.errors.oauth.InvalidGoogleId;
import legal_website.common.errors.oauth.OAuthConfigurationException;
import legal_website.common.errors.oauth.OAuthAccountAlreadyLinkedException;
import legal_website.common.errors.oauth.OAuthAccountNotFoundException;
import legal_website.common.errors.oauth.OAuthProviderUnavailableException;
import legal_website.common.errors.user.InactiveUserException;
import legal_website.common.errors.user.UserAlreadyExistsException;
import legal_website.common.errors.user.UserNotFoundException;
import legal_website.common.errors.email.EmailAlreadyVerified;
import legal_website.common.errors.token.InvalidCredentialsException;
import legal_website.common.errors.token.InvalidFlowTokenException;
import legal_website.common.errors.token.RefreshTokenNotFoundException;
import legal_website.common.errors.token.RefreshTokenRevokedException;
import legal_website.common.errors.token.TokenValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ApiErrorResponse buildErrorResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        ApiErrorResponse response = new ApiErrorResponse();
        response.setStatus(status.value());
        response.setError(status.getReasonPhrase());
        response.setMessage(message);
        response.setPath(request.getRequestURI());
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationExceptions(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ){
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Validation failed");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(HttpStatus.BAD_REQUEST, message, request));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> userNotFoundHandler(
        UserNotFoundException ex,
        HttpServletRequest request
    ){
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> invalidCredentialsHandler(
            InvalidCredentialsException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request));
    }

    @ExceptionHandler(TokenValidationException.class)
    public ResponseEntity<ApiErrorResponse> tokenValidationHandler(
            TokenValidationException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> userAlreadyExistsHandler(
            UserAlreadyExistsException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request));
    }

    @ExceptionHandler(InactiveUserException.class)
    public ResponseEntity<ApiErrorResponse> inactiveUserHandler(
            InactiveUserException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request));
    }

    @ExceptionHandler(OAuthConfigurationException.class)
    public ResponseEntity<ApiErrorResponse> oAuthConfigurationHandler(
            OAuthConfigurationException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request));
    }

    @ExceptionHandler(OAuthProviderUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> oAuthProviderUnavailableHandler(
            OAuthProviderUnavailableException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> genericHandler(
            Exception ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request));
    }
    @ExceptionHandler(InvalidFlowTokenException.class)
    public ResponseEntity<ApiErrorResponse> invalidFlowTokenHandler(
            InvalidFlowTokenException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request));
    }
    @ExceptionHandler(OAuthAccountAlreadyLinkedException.class)
    public ResponseEntity<ApiErrorResponse> oAuthAccountAlreadyLinkedHandler(
            OAuthAccountAlreadyLinkedException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request));
    }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> accessDeniedHandler(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)        
                .body(buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request));
    }
    @ExceptionHandler(RefreshTokenNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> refreshTokenNotFoundHandler(
            RefreshTokenNotFoundException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request));    
    }
    @ExceptionHandler(RefreshTokenRevokedException.class)
    public ResponseEntity<ApiErrorResponse> refreshTokenRevokedHandler(
            RefreshTokenRevokedException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request));
    }
    @ExceptionHandler(OAuthAccountNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> oAuthAccountNotFoundHandler(
            OAuthAccountNotFoundException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request));
    }
    @ExceptionHandler(InvalidGoogleId.class)
    public ResponseEntity<ApiErrorResponse> invalidGoogleIdHandler(
            InvalidGoogleId ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request));
    }
    @ExceptionHandler(EmailAlreadyVerified.class)
    public ResponseEntity<ApiErrorResponse> emailAlreadyVerifiedHandler(
            EmailAlreadyVerified ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request));
    }
   
}
