package app.bys.bys_api.controller;

import app.bys.bys_api.model.dto.AuthRequest;
import app.bys.bys_api.model.dto.AuthResponse;
import app.bys.bys_api.model.dto.FinalUserDto;
import app.bys.bys_api.model.dto.ServiceProviderDto;
import app.bys.bys_api.service.AuthService;
import app.bys.bys_api.service.OtpService;
import app.bys.bys_api.validation.OnCreate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest authRequest) {
        return authService.login(authRequest);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String email, @RequestParam String otp) {
        try {
            authService.verifyEmail(email, otp);
            return ResponseEntity.ok("Email verificado exitosamente");
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", ex.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            otpService.resendOtp(email);
            return ResponseEntity.ok().body(Map.of(
                    "message", "Nuevo OTP enviado con éxito",
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

//    @PostMapping("/password/recovery/request")
//    public ResponseEntity<Void> passwordRecoveryRequest(@RequestBody EmailDto emailDto) {
//        authService.passwordRecoveryRequest(emailDto);
//        return new ResponseEntity<>(HttpStatus.OK);
//    }
//
//    @PostMapping("/password/recovery/change")
//    public ResponseEntity<Void> changeRecoveryPassword(Authentication authentication,
//                                                       @RequestBody ChangePasswordDto changePasswordDto) {
//        authService.changeRecoveryPassword(authentication, changePasswordDto);
//        return new ResponseEntity<>(HttpStatus.OK);
//    }
//
//    @PostMapping("/password/change")
//    public ResponseEntity<Void> changePassword(Authentication authentication,
//                                               @RequestBody ChangePasswordDto changePasswordDto) {
//        authService.changePassword(authentication, changePasswordDto);
//        return new ResponseEntity<>(HttpStatus.OK);
//    }


}


