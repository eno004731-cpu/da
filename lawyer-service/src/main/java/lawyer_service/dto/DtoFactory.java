package lawyer_service.dto;

import java.time.Instant;

import org.springframework.stereotype.Service;

import lawyer_service.repo_entity.LawyerEntity;
import lawyer_service.repo_entity.enums.Role;
import lawyer_service.repo_entity.enums.StatusLawyer;
import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class DtoFactory {
    public AuthResponse buildAuthResponse(LawyerEntity lawyer,String refreshToken, String accessToken){
        Instant now = Instant.now();
        AuthResponse authResponse = new AuthResponse();
        authResponse.setCreatedAt(now);
        authResponse.setLastLoginAt(now);
        authResponse.setLaweyr(lawyer.getId());
        authResponse.setLawyerInformation(buildInformation(lawyer));
        authResponse.setRole(Role.LAWYER);
        authResponse.setStatus(StatusLawyer.PENDING_VERIFICATION);
        authResponse.setUpdatedAt(now);
        authResponse.setRefreshToken(refreshToken);
        authResponse.setAccessToken(accessToken);
        return authResponse;
    }
    private LawyerInformation buildInformation(LawyerEntity lawyer){
        LawyerInformation lawyerInformation = new LawyerInformation();
        lawyerInformation.setEmail(lawyer.getEmail());
        lawyerInformation.setFirstName(lawyer.getFirstName());
        lawyerInformation.setLastName(lawyer.getLastName());
        lawyerInformation.setMiddleName(lawyer.getMiddleName());
        lawyerInformation.setPhone(lawyer.getPhone());
        lawyerInformation.setSpecialization(lawyer.getSpecialization());
        return lawyerInformation;
    }
}
