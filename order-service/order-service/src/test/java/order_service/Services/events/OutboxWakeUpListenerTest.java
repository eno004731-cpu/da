package order_service.Services.events;

import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import order_service.Services.catalog.SendEventForGetServiceName;

@ExtendWith(MockitoExtension.class)
class OutboxWakeUpListenerTest {

    @Mock
    private SendEventForGetServiceName sendEventForGetServiceName;

    @InjectMocks
    private OutboxWakeUpListener outboxWakeUpListener;

    @Test
    void onOutboxWakeUp_triggersImmediateBatchProcessing() {
        outboxWakeUpListener.onOutboxWakeUp(new OutboxWakeUpEvent(UUID.randomUUID()));

        verify(sendEventForGetServiceName).processAvailableEvents();
    }
}
