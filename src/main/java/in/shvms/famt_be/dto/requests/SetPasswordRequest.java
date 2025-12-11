package in.shvms.famt_be.dto.requests;

import lombok.Data;

/**
 * Request to set new password
 */
@Data
public class SetPasswordRequest {
    private String email;
    private String newPassword;
}
