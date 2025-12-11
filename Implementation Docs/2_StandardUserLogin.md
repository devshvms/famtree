# Standard User Login Flow - Complete Implementation Guide

## Overview

This implementation provides a secure multi-step authentication process for Standard Users:

1. **Admin creates user** → Random PIN generated + Welcome email sent
2. **User first login** → Validates PIN + Sends OTP to email
3. **Email verification** → User enters OTP
4. **Password setup** → User sets secure password
5. **Account activation** → User can now login normally

## Flow Diagram

```
┌─────────────────┐
│  TENANT_ADMIN   │
│  Creates User   │
└────────┬────────┘
         │
         ├──► Random 6-digit PIN generated
         ├──► Email sent with PIN
         └──► User status: inactive
                │
                ▼
┌─────────────────────────────┐
│  STANDARD_USER              │
│  First Login with PIN       │
└────────┬────────────────────┘
         │
         ├──► Validates PIN
         ├──► Generates 6-digit OTP
         └──► Sends OTP to email (valid 10 mins)
                │
                ▼
┌─────────────────────────────┐
│  User Enters OTP            │
└────────┬────────────────────┘
         │
         ├──► Validates OTP
         ├──► Marks email as verified
         └──► Prompts for new password
                │
                ▼
┌─────────────────────────────┐
│  User Sets New Password     │
└────────┬────────────────────┘
         │
         ├──► Validates password strength
         ├──► Sets new password
         ├──► Activates account
         └──► Sends confirmation email
                │
                ▼
┌─────────────────────────────┐
│  Account Active             │
│  Can login normally         │
└─────────────────────────────┘
```

## API Endpoints

### 1. Admin Creates Standard User

**Endpoint:** `POST /api/auth/create-standard-user`

**Authorization:** Requires `TENANT_ADMIN` role

**Request:**
```json
{
  "email": "john.doe@example.com",
  "displayName": "John Doe"
}
```

**Response (Success):**
```json
{
  "message": "User created successfully. Welcome email with temporary PIN sent.",
  "user": {
    "id": "user123",
    "tenantId": "tenant456",
    "username": "john.doe@example.com",
    "roles": ["STANDARD_USER"],
    "isActive": false,
    "emailVerified": false,
    "firstTimeLogin": true
  }
}
```

**Email Sent:**
```
Subject: Welcome to Family Tree Application

Hello John Doe,

Welcome to Family Tree Application! Your account has been created by your family administrator.

Your login credentials:
Email: john.doe@example.com
Temporary PIN: 847291

For security reasons, you will need to:
1. Login with your email and temporary PIN
2. Verify your email address with an OTP
3. Set a new secure password

Please login at your earliest convenience to complete the setup.

Best regards,
Family Tree Application Team
```

### 2. First-Time Login with PIN

**Endpoint:** `POST /api/auth/first-login`

**Authorization:** None (public endpoint)

**Request:**
```json
{
  "email": "john.doe@example.com",
  "pin": "847291"
}
```

**Response (Success):**
```json
{
  "message": "OTP sent to your email",
  "email": "john.doe@example.com",
  "expiresIn": "10 minutes"
}
```

**Email Sent:**
```
Subject: Family Tree Application - Email Verification OTP

Hello john.doe@example.com,

Your email verification code is: 562834

This code will expire in 10 minutes.

If you didn't request this code, please ignore this email.

Best regards,
Family Tree Application Team
```

**Error Responses:**
- `401 Unauthorized`: Invalid email or PIN
- `403 Forbidden`: Account locked due to too many failed attempts
- `400 Bad Request`: Account already activated

### 3. Verify OTP

**Endpoint:** `POST /api/auth/verify-otp`

**Authorization:** None (public endpoint)

**Request:**
```json
{
  "email": "john.doe@example.com",
  "otp": "562834"
}
```

**Response (Success):**
```json
{
  "message": "Email verified successfully. Please set your password.",
  "email": "john.doe@example.com"
}
```

**Error Responses:**
- `400 Bad Request`: Invalid OTP, OTP expired, or no verification in progress

### 4. Resend OTP (Optional)

**Endpoint:** `POST /api/auth/resend-otp`

**Authorization:** None (public endpoint)

**Request:**
```json
{
  "email": "john.doe@example.com"
}
```

**Response (Success):**
```json
{
  "message": "New OTP sent to your email",
  "email": "john.doe@example.com",
  "expiresIn": "10 minutes"
}
```

### 5. Set New Password

**Endpoint:** `POST /api/auth/set-password`

**Authorization:** None (public endpoint)

**Request:**
```json
{
  "email": "john.doe@example.com",
  "newPassword": "SecureP@ssw0rd123!"
}
```

**Password Requirements:**
- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one digit
- At least one special character (!@#$%^&*()_+-=[]{}|;:,.<>?)

**Response (Success):**
```json
{
  "message": "Password set successfully. Your account is now active.",
  "email": "john.doe@example.com"
}
```

**Email Sent:**
```
Subject: Family Tree Application - Password Updated Successfully

Hello john.doe@example.com,

Your password was successfully updated on 2024-12-11 14:30:45.

If you didn't make this change, please contact your administrator immediately.

Best regards,
Family Tree Application Team
```

**Error Responses:**
- `400 Bad Request`: Weak password, email not verified, or account already activated

### 6. Regular Login (After Activation)

**Endpoint:** `POST /api/auth/login`

**Authorization:** None (public endpoint)

**Request:**
```json
{
  "username": "john.doe@example.com",
  "password": "SecureP@ssw0rd123!"
}
```

**Response (Success):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "john.doe@example.com",
  "tenantId": "tenant456",
  "userId": "user123",
  "roles": [
    {
      "authority": "ROLE_STANDARD_USER"
    }
  ]
}
```

## Security Features

### 1. Account Locking
- After 5 failed login attempts, account is locked for 30 minutes
- Applies to both PIN validation and password login
- Automatic unlock after lockout period

### 2. OTP Expiration
- OTP valid for 10 minutes only
- Can request new OTP if expired
- Old OTP invalidated when new one is generated

### 3. Password Strength Validation
- Enforced at server-side
- Must meet all complexity requirements
- Prevents weak passwords

### 4. Email Verification
- Ensures user owns the email address
- Required before password can be set
- Cannot be bypassed

### 5. Audit Logging
All actions are logged:
- User creation
- First-time login attempts
- OTP generation/verification
- Password changes
- Account activation

## Database Schema Updates

### User Collection (MongoDB)

```javascript
{
  "_id": ObjectId("..."),
  "tenantId": "tenant456",
  "username": "john.doe@example.com",
  "password": "$2a$10$...", // Bcrypt hashed
  "roles": ["STANDARD_USER"],
  "isActive": true,
  "emailVerified": true,
  "firstTimeLogin": false,
  "verificationToken": null,
  "tokenExpiry": null,
  "lastLoginAt": ISODate("2024-12-11T14:35:00Z"),
  "passwordLastChangedAt": ISODate("2024-12-11T14:30:00Z"),
  "failedLoginAttempts": 0,
  "accountLockedUntil": null
}
```

## Configuration Setup

### 1. Update application.properties

```properties
# Email Configuration (Gmail example)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-specific-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true

# Application Name
app.name=Family Tree Application
```

### 2. Gmail App Password Setup

For Gmail, you need to:
1. Enable 2-Factor Authentication
2. Generate an App Password
3. Use the App Password (not your regular password)

Steps:
- Go to Google Account → Security
- Enable 2-Step Verification
- Search for "App Passwords"
- Select "Mail" and your device
- Copy the 16-character password
- Use this in `spring.mail.password`

### 3. Update pom.xml

Add email dependency:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

## Testing Guide

### Test Scenario 1: Successful Flow

```bash
# Step 1: Admin creates user
curl -X POST http://localhost:8080/api/auth/create-standard-user \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "displayName": "Test User"
  }'

# Check email for PIN (e.g., 123456)

# Step 2: First login with PIN
curl -X POST http://localhost:8080/api/auth/first-login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "pin": "123456"
  }'

# Check email for OTP (e.g., 789012)

# Step 3: Verify OTP
curl -X POST http://localhost:8080/api/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "otp": "789012"
  }'

# Step 4: Set password
curl -X POST http://localhost:8080/api/auth/set-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "newPassword": "SecureP@ss123!"
  }'

# Step 5: Regular login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test@example.com",
    "password": "SecureP@ss123!"
  }'
```

### Test Scenario 2: Invalid PIN

```bash
curl -X POST http://localhost:8080/api/auth/first-login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "pin": "wrong-pin"
  }'

# Expected: 401 Unauthorized
# Response: {"error": "Invalid credentials"}
```

### Test Scenario 3: Expired OTP

```bash
# Wait 11 minutes after receiving OTP, then:
curl -X POST http://localhost:8080/api/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "otp": "789012"
  }'

# Expected: 400 Bad Request
# Response: {"error": "OTP has expired. Please request a new one."}
```

### Test Scenario 4: Weak Password

```bash
curl -X POST http://localhost:8080/api/auth/set-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "newPassword": "weak"
  }'

# Expected: 400 Bad Request
# Response: {"error": "Password must contain at least one uppercase..."}
```

## Troubleshooting

### Issue: Email not received

**Possible Causes:**
1. Incorrect SMTP configuration
2. Gmail blocking less secure apps
3. Email in spam folder
4. Invalid email address

**Solutions:**
1. Check application logs for email sending errors
2. Verify Gmail App Password is correct
3. Check spam/junk folder
4. Test with a different email provider

### Issue: OTP validation fails

**Possible Causes:**
1. OTP expired (>10 minutes)
2. Typo in OTP entry
3. Using old OTP after requesting new one

**Solutions:**
1. Request new OTP
2. Copy-paste OTP instead of typing
3. Verify OTP in email matches input

### Issue: Account locked

**Cause:** Too many failed login attempts

**Solution:**
- Wait 30 minutes for automatic unlock
- Or admin can manually unlock by updating user in database:
  ```javascript
  db.users.updateOne(
    { username: "user@example.com" },
    { 
      $set: { 
        accountLockedUntil: null,
        failedLoginAttempts: 0 
      } 
    }
  )
  ```

## Best Practices

1. **PIN Security**
   - PINs are immediately discarded after first use
   - Never log or display PINs
   - Encourage users to complete setup quickly

2. **OTP Security**
   - Short expiration time (10 minutes)
   - Single-use only
   - Stored hashed, not plain text

3. **Password Policy**
   - Enforce strong passwords
   - Regular password rotation (optional)
   - Password history (prevent reuse)

4. **Email Notifications**
   - Send confirmation for all security actions
   - Include timestamp and IP address
   - Clear instructions for suspicious activity

5. **Rate Limiting**
   - Implement rate limiting on auth endpoints
   - Prevent brute force attacks
   - Use CAPTCHA for repeated failures

## Future Enhancements

1. **SSO Integration** (Google, Facebook, etc.)
2. **SMS OTP** as alternative to email
3. **Biometric authentication** for mobile
4. **Remember device** functionality
5. **Password strength meter** in frontend
6. **Two-factor authentication** option
7. **Backup codes** for account recovery

## Support

For issues or questions:
1. Check audit logs in MongoDB
2. Review application logs
3. Contact system administrator
4. Refer to security documentation