package in.shvms.famt_be.dto.requests;

import lombok.Data;

/**
 * Request to resend OTP
 */
@Data
public class ResendOTPRequest {
    private String email;
}