package my_jira.orders.order;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class AnswerDto {
    private Long orderId;
    private String status;
    private String publicCode;
}
