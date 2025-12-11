package in.shvms.famt_be.dto.requests;

import lombok.Data;

/**
 * Request for admin to create standard user
 */
@Data
public class CreateStandardUserRequest {
    private String email;
    private String displayName;
}
