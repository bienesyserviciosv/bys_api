package app.bys.bys_api.controller;

import app.bys.bys_api.model.dto.*;
import app.bys.bys_api.service.AuthService;
import app.bys.bys_api.service.OtpService;
import app.bys.bys_api.validation.OnCreate;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static app.bys.bys_api.service.OtpService.MAX_RESEND_ATTEMPTS;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    @PostMapping("/final_user/register")
    public ResponseEntity<Map<String, Object>> registerFinalUser(@Validated({OnCreate.class}) @RequestBody FinalUserDto finalUserDto) {
        FinalUserDto userRegistered = authService.registerFinalUser(finalUserDto);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Final User register successfully");
        response.put("user", userRegistered);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/service_provider/register")
    public ResponseEntity<Map<String, Object>> registerServiceProvider(@Validated({OnCreate.class}) @RequestBody ServiceProviderDto serviceProviderDto) {
        ServiceProviderDto providerRegistered = authService.registerServiceProvider(serviceProviderDto);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Service Provider register successfully");
        response.put("user", providerRegistered);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody AuthRequestDto authRequestDto) {
        return authService.login(authRequestDto);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String email, @RequestParam String otp) {
        try {
            authService.verifyEmail(email, otp);
            return ResponseEntity.ok("Email verified successfully");
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", ex.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody @Valid EmailDto emailDto) {
        try {
            String email = emailDto.getEmail();
            otpService.resendOtp(email);
            return ResponseEntity.ok().body(Map.of(
                    "message", "New OTP sent successfully",
                    "status", "SUCCESS",
                    "attemptsLeft", MAX_RESEND_ATTEMPTS - otpService.getResendAttempts(email)
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", e.getMessage(),
                    "status", "ERROR"
            ));
        }
    }

    @GetMapping("/login-user")
    public void redirectToGoogleUser(HttpServletResponse response) throws IOException {
        response.sendRedirect("/api/oauth2/authorization/google-user");
    }

    @GetMapping("/login-provider")
    public void redirectToGoogleProvider(HttpServletResponse response) throws IOException {
        response.sendRedirect("/api/oauth2/authorization/google-provider");
    }


//    @PostMapping("/password/recovery/request")
//    public ResponseEntity<Void> passwordRecoveryRequest(@RequestBody EmailDto emailDto) {
//        authService.passwordRecoveryRequest(emailDto);
//        return new ResponseEntity<>(HttpStatus.OK);
//    }
//

    @PostMapping("/password/recovery/verify")
    public ResponseEntity<?> verifyOtpForPasswordReset(@RequestParam String email, @RequestParam String otp) {
        try {
            authService.verifyOtpForPasswordReset(email, otp);
            return ResponseEntity.ok("OTP verified for recover password successfully");
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", ex.getMessage(), "timestamp", LocalDateTime.now()));
        }
    }

    @PostMapping("/password/recovery/change")
    public ResponseEntity<String> resetPassword(@Validated({OnCreate.class}) @RequestBody ResetPasswordRequest resetPasswordRequest) {
        authService.resetPassword(resetPasswordRequest.getEmail(), resetPasswordRequest.getPassword());
        return new ResponseEntity<>("Password reset successfully", HttpStatus.OK);
    }

    @PostMapping("/password/change")
    public ResponseEntity<?> changePassword(Authentication auth, @Valid @RequestBody ChangePasswordDto changePasswordDto) {

        try {
            authService.changePassword(auth.getName(), changePasswordDto);
            return ResponseEntity.ok("Password changed successfully");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

}


