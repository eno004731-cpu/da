package my_jira.orders.order.patchOrder;

import lombok.Data;

@Data
public class PatchDto {
    private String serviceCode;
    private String clientName;
    private String contact;
    private String companyName;
    private String description;
}
