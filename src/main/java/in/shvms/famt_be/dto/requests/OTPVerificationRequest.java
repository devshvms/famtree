package in.shvms.famt_be.dto.requests;

import lombok.Data;

/**
 * Request for OTP verification
 */
@Data
public class OTPVerificationRequest {
    private String email;
    private String otp;
}
