package lawyer_service.services;


import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;

import lawyer_service.dto.AuthRequest;
import lawyer_service.dto.AuthResponse;
import lawyer_service.dto.DtoFactory;
import lawyer_service.dto.EntityFactory;
import lawyer_service.repo_entity.LawyerEntity;
import lawyer_service.repo_entity.LawyerRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class RegisterService {
    private final LawyerRepo lawyerRepo;
    private final EntityFactory entityFactory;
    private final BCryptPasswordEncoder passwordEncoder;
    private final DtoFactory dtoFactory;
    private final TokenService tokenService;
    @Transactional
    public AuthResponse reg(AuthRequest authRequest){
        if (lawyerRepo.existsByEmail(authRequest.getEmail())) {
            throw new HttpStatusCodeException(HttpStatus.CONFLICT,"Уже есть такой аккаунт"){};
        }
        LawyerEntity lawyer = entityFactory.buildLawyerEntity(authRequest, encodePass(authRequest.getPassword()));
        LawyerEntity savedLawyer = lawyerRepo.save(lawyer);
        String refreshToken = tokenService.buidlRefreshToken(savedLawyer);
        String accessToken = tokenService.buildAccessToken(savedLawyer);
        tokenService.saveToken(savedLawyer, refreshToken, authRequest.getIp(), authRequest.getAgentId());
        AuthResponse authResponse = dtoFactory.buildAuthResponse(savedLawyer, refreshToken, accessToken);
        return authResponse;
    }
    private String encodePass(String password){
        return passwordEncoder.encode(password);
    }
}
