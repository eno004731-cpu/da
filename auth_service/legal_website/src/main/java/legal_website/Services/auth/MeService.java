package legal_website.Services.auth;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import legal_website.Dto.MeResponse;
import legal_website.EntityAndRepo.Auth.OAuthAccountEntity;
import legal_website.EntityAndRepo.Auth.OAuthAccountRepo;
import legal_website.EntityAndRepo.Auth.UserEntity;
import legal_website.EntityAndRepo.Auth.UserRepo;
import legal_website.common.errors.User.InactiveUserException;
import legal_website.common.errors.User.UserNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MeService {
    private final UserRepo userRepo;
    private final OAuthAccountRepo oAuthAccountRepo;
    @Transactional(readOnly = true)
    public MeResponse getMe(Long userId){
        UserEntity user = userRepo.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("нет пользователя"));

        if (!user.isActive()) {
            throw new InactiveUserException("не активный пользователь");
        }

        
        return MakeMeResponse(user);
    }
    public MeResponse MakeMeResponse(UserEntity user){
        MeResponse response = new MeResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setCompanyName(user.getCompanyName());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());
        List<OAuthAccountEntity> oAuthAccountEntities = oAuthAccountRepo.findAllByUser(user);
        boolean hasPassword = user.getPasswordHash() != null && !user.getPasswordHash().isBlank();
        boolean isOAuthUser = !oAuthAccountEntities.isEmpty();

        boolean needsPasswordSetup = isOAuthUser && !hasPassword;

        boolean profileIncomplete =
                user.getFullName() == null || user.getFullName().isBlank()
            || user.getEmail() == null || user.getEmail().isBlank();

        boolean requiresProfileCompletion = profileIncomplete || needsPasswordSetup;
        response.setAuthProviders(oAuthAccountEntities.stream().map(OAuthAccountEntity::getProvider).collect(Collectors.toList()));
        response.setIsOAuthUser(isOAuthUser);
        response.setHasPassword(hasPassword);
        response.setNeedsPasswordSetup(needsPasswordSetup);
        response.setRequiresProfileCompletion(requiresProfileCompletion);
        return response;
    }
}
