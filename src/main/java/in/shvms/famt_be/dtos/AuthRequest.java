package in.shvms.famt_be.dtos;

import lombok.Data;

@Data
public class AuthRequest {

    private String username;
    private String password;

}
