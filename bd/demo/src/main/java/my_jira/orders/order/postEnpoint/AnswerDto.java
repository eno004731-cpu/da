package my_jira.orders.order.postEnpoint;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class AnswerDto {
    private Long orderId;
    private String clientName;
    private String contact;
    private String companyName;
    private String status;
    private String publicCode;
    // private List<String> errors;
}
