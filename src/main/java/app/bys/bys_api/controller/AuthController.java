package app.bys.bys_api.controller;

import app.bys.bys_api.model.dto.*;
import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.repository.FinalUserRepository;
import app.bys.bys_api.repository.ServiceProviderRepository;
import app.bys.bys_api.service.AuthService;
import app.bys.bys_api.service.FinalUserService;
import app.bys.bys_api.service.OtpService;
import app.bys.bys_api.utils.JwtUtil;
import app.bys.bys_api.validation.OnCreate;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static app.bys.bys_api.service.OtpService.MAX_RESEND_ATTEMPTS;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;
    private final FinalUserService finalUserService;
    private final FinalUserRepository finalUserRepo;
    private final ServiceProviderRepository serviceProviderRepo;
    private final JwtUtil jwtUtil;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @PostMapping("/final_user/register")
    public ResponseEntity<FinalUserDto> registerFinalUser(@Validated({OnCreate.class})
                                                          @RequestPart(name = "user") FinalUserDto finalUserDto,
                                                          @RequestPart(name = "profile_picture", required = false) MultipartFile profilePicture) {
        FinalUserDto userRegistered = authService.registerFinalUser(finalUserDto, profilePicture);
        return ResponseEntity.status(HttpStatus.CREATED).body(userRegistered);
    }

    @PostMapping("/service_provider/register")
    public ResponseEntity<ServiceProviderWithPictureDto> registerServiceProvider(@Validated({OnCreate.class}) @RequestPart(name = "provider") ServiceProviderDto serviceProviderDto,
                                                                                 @RequestPart(name = "profile_picture", required = false) MultipartFile profilePicture,
                                                                                 @RequestPart(name = "work_picture_set", required = false) MultipartFile[] workPictureSet) {
        ServiceProviderWithPictureDto providerRegistered = authService.registerServiceProvider(serviceProviderDto, profilePicture, workPictureSet);
        return ResponseEntity.status(HttpStatus.CREATED).body(providerRegistered);
    }

//    @PostMapping("/admin/register")
//    public ResponseEntity<FinalUserDto> registerAdmin(@Validated({OnCreate.class}) @RequestBody FinalUserDto finalUserDto) {
//        FinalUserDto userRegistered = authService.registerAdmin(finalUserDto);
//        return ResponseEntity.status(HttpStatus.CREATED).body(userRegistered);
//    }

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
            boolean exists = finalUserRepo.existsByEmail(emailDto.getEmail()) || serviceProviderRepo.existsByEmail(emailDto.getEmail());
            if (!exists) {
                throw new RuntimeException("The OTP can not be resent");
            }
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

    @PostMapping("/google")
    public ResponseEntity<?> authenticateWithGoogle(@RequestBody GoogleAuthRequest request) {
        try {
            GoogleIdToken.Payload payload = verifyGoogleToken(request.getIdToken());

            FinalUser user = finalUserService.findOrCreateUserFromGoogle(payload);

            List<GrantedAuthority> authorities = user.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority(role.getName()))
                    .collect(Collectors.toList());

            String jwt = jwtUtil.generateToken(user.getEmail(), authorities);

            log.info("Token audience: {}", payload.getAudience());

            return ResponseEntity.ok(new JwtAuthenticationResponse(jwt));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    private GoogleIdToken.Payload verifyGoogleToken(String idTokenString) throws Exception {

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken == null) {
            throw new RuntimeException("Token inválido");
        }

        return idToken.getPayload();
    }
}
