package in.shvms.famt_be.dto.requests;

import lombok.Data;

/**
 * Request for first-time login with PIN
 */
@Data
public class FirstLoginRequest {
    private String email;
    private String pin;
}





