package in.shvms.famt.famtbe.dtos;

import lombok.Data;

@Data
public class UserCreationRequest {

    private String username;
    private String password;
    private String tenantId;

}
