package legal_website.services.delete;

import java.time.LocalDateTime;

import legal_website.dto.DeletePayload;
import legal_website.persistence.auth.UserDeletionStatus;
import legal_website.persistence.deletion.UserDeletionProcessEntity;
import legal_website.persistence.deletion.UserDeletionProcessRepo;
import legal_website.persistence.outbox_events.OutboxEventEntity;
import legal_website.persistence.outbox_events.OutboxEventsRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import legal_website.persistence.auth.OAuthAccountRepo;
import legal_website.persistence.auth.UserEntity;
import legal_website.persistence.auth.UserRepo;
import legal_website.persistence.jwt.JwtRepo;
import legal_website.common.errors.user.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class DelService {
    private final UserRepo userRepo;
    private final JwtRepo jwtRepo;
    private final OAuthAccountRepo oAuthAccountRepo;
    private final UserDeletionProcessRepo userDeletionProcessRepo;
    private final OutboxEventsRepo outboxEventsRepo;
    private final ObjectMapper objectMapper;
    @Transactional
    public void delUser(Long userId){
        UserEntity user = userRepo.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("нет пользователя"));

        oAuthAccountRepo.deleteAllByUser(user);
        jwtRepo.deleteAllByUser(user);
        user.setActive(false);
        user.setEmail(null);
        user.setPhone(null);
        user.setDeletionStatus(UserDeletionStatus.DELETION_IN_PROGRESS);
        user.setUpdatedAt(LocalDateTime.now());
        user.setDeletionProcess(getUserDeletionProcessEntity(user));
        userRepo.save(user);
        saveNewOrderOutboxEvent(
                userId,
                DeleteOutboxEventType.DELETE_ALL_ORDERS
        );
        saveNewOrderOutboxEvent(
                userId,
                DeleteOutboxEventType.DELETE_ALL_DOCUMENTS
        );
    }

    private void saveNewOrderOutboxEvent(
            Long userId,
            DeleteOutboxEventType eventType
    ){
        OutboxEventEntity outboxEventEntity = new OutboxEventEntity();
        outboxEventEntity.setAggregateId(userId.toString());
        outboxEventEntity.setAggregateType("USER");
        DeletePayload payload = new DeletePayload();
        payload.setId(userId);
        try {

            outboxEventEntity.setPayload(objectMapper.valueToTree(payload));
        }catch (Exception e){
            throw new RuntimeException("");
        }
        outboxEventEntity.setEventType(eventType.name());
        outboxEventEntity.setStatus("NEW");
        outboxEventEntity.setRetryCount(0);
        outboxEventEntity.setCreatedAt(LocalDateTime.now());
        // Событие и изменение пользователя сохраняются в одной транзакции.
        outboxEventsRepo.save(outboxEventEntity);

    }
    private UserDeletionProcessEntity  getUserDeletionProcessEntity(UserEntity user){
        UserDeletionProcessEntity userDeletionProcessEntity = new UserDeletionProcessEntity();
        userDeletionProcessEntity.setUser(user);
        userDeletionProcessEntity.setStatus(UserDeletionStatus.DELETION_IN_PROGRESS);
        userDeletionProcessEntity.setRetryCount(0);
        userDeletionProcessEntity.setRequestedAt(LocalDateTime.now());
        userDeletionProcessEntity.setUpdatedAt(LocalDateTime.now());
        userDeletionProcessRepo.save(userDeletionProcessEntity);
        return userDeletionProcessEntity;
    }
}
