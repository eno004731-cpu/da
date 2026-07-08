package lawyer_service.dto;



import java.util.UUID;

import lombok.Data;

@Data
public class LawyerInformation {
    
    private String email;
    private String firstName;
    private String lastName;
    private String middleName;
    private String phone;
    private String bar_number;
    private String specialization;
    
}
