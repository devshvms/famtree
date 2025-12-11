package in.shvms.famt_be.dto;

import lombok.Data;

@Data
public class UserCreationRequest {

    private String username;
    private String password;
    private String tenantId;

}
