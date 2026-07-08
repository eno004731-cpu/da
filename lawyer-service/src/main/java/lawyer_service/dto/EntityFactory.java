package lawyer_service.dto;

import java.time.Instant;

import org.springframework.stereotype.Service;

import lawyer_service.repo_entity.LawyerEntity;
import lawyer_service.repo_entity.enums.Role;
import lawyer_service.repo_entity.enums.StatusLawyer;
import lawyer_service.repo_entity.jwt.JwtTokenEntity;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EntityFactory {
    public JwtTokenEntity buildJwtEntity(
            LawyerEntity lawyer,
            long expiresDay,
            String hashToken,
            String ip,
            String agentId
        ){
        Instant now = Instant.now();
        JwtTokenEntity tokenEntity = new JwtTokenEntity();
        tokenEntity.setCreatedAt(now);
        tokenEntity.setExpiresAt(now.plusSeconds(expiresDay));
        tokenEntity.setLastUsedAt(now);
        tokenEntity.setLawyer(lawyer);
        tokenEntity.setRevoked(false);
        tokenEntity.setSessionBlocked(false);
        tokenEntity.setTokenHash(hashToken);
        tokenEntity.setIpAddress(ip);
        tokenEntity.setUserAgent(agentId);
        return tokenEntity;
    }
    public LawyerEntity buildLawyerEntity(AuthRequest authRequest,String hashPass){
        Instant now = Instant.now();
        LawyerEntity lawyer = new LawyerEntity();
        lawyer.setEmail(authRequest.getEmail());
        lawyer.setPasswordHash(hashPass);
        lawyer.setPhone(authRequest.getPhone());
        lawyer.setCreatedAt(now);
        lawyer.setFirstName(authRequest.getFirstName());
        lawyer.setLastLoginAt(now);
        lawyer.setLastName(authRequest.getLastName());
        lawyer.setMiddleName(authRequest.getMiddleName());
        lawyer.setRole(Role.LAWYER);
        lawyer.setStatus(StatusLawyer.PENDING_VERIFICATION);
        lawyer.setSpecialization(authRequest.getSpecialization());
        return lawyer;
    }
}
