package app.bys.bys_api.service;

import app.bys.bys_api.error.DuplicateEmailException;
import app.bys.bys_api.error.DuplicatePhoneException;
import app.bys.bys_api.error.EmailNotVerifiedException;
import app.bys.bys_api.error.ErrorMessage;
import app.bys.bys_api.mapper.FinalUserMapper;
import app.bys.bys_api.mapper.ServiceProviderMapper;
import app.bys.bys_api.mapper.SpecializationMapper;
import app.bys.bys_api.model.dto.*;
import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.model.enums.MembershipType;
import app.bys.bys_api.model.enums.UserStatus;
import app.bys.bys_api.repository.FinalUserRepository;
import app.bys.bys_api.repository.ServiceProviderRepository;
import app.bys.bys_api.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final FinalUserRepository finalUserRepo;
    private final ServiceProviderRepository serviceProviderRepo;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;
    private final FinalUserMapper userMapper;
    private final SpecializationMapper specializationMapper;
    private final ServiceProviderMapper serviceProviderMapper;
    private final OtpService otpService;
    private final UserDetailsService userDetailsService;

    public FinalUserDto registerFinalUser(FinalUserDto dto) {

        if (dto.getPhoneNumber() != null && !dto.getPhoneNumber().isBlank()) {
            if (finalUserRepo.existsByPhoneNumber(dto.getPhoneNumber())) {
                throw new DuplicatePhoneException("The phone number is already registered");
            }
            //handlePhoneOtp(dto.getPhoneNumber());
        }

        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            if (finalUserRepo.existsByEmail(dto.getEmail()) || serviceProviderRepo.existsByEmail(dto.getEmail())) {
                throw new DuplicateEmailException("The email is already registered");
            }
            handleEmailOtp(dto.getEmail());
        }

        FinalUser user = FinalUser.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .password(passwordEncoder.encode(dto.getPassword()))
                .phoneVerified(false)
                .emailVerified(false)
                .status(UserStatus.ACTIVE)
                .roles(Set.of(roleService.getRoleOrThrow("ROLE_USER")))
                .registrationDate(LocalDateTime.now())
                .build();

        return userMapper.entityToDto(finalUserRepo.save(user));
    }

    public ServiceProviderDto registerServiceProvider(ServiceProviderDto dto) {

        if (dto.getPhoneNumber() != null && !dto.getPhoneNumber().isBlank()) {
            if (serviceProviderRepo.existsByPhoneNumber(dto.getPhoneNumber())) {
                throw new DuplicatePhoneException("The phone number is already registered");
            }
            //handlePhoneOtp(dto.getPhoneNumber());
        }

        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            if (serviceProviderRepo.existsByEmail(dto.getEmail()) || finalUserRepo.existsByEmail(dto.getEmail())) {
                throw new DuplicateEmailException("The email is already registered");
            }
            handleEmailOtp(dto.getEmail());
        }

        ServiceProvider provider = ServiceProvider.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .password(passwordEncoder.encode(dto.getPassword()))
                .status(UserStatus.ACTIVE)
                .experience(dto.getExperience())
                .specializations(specializationMapper.setDtoToEntitySet(dto.getSpecializations()))
                .emailVerified(false)
                .phoneVerified(false)
                .membershipType(MembershipType.NOT_VERIFIED)
                .verified(false)
                .address(dto.getAddress())
                .roles(Set.of(roleService.getRoleOrThrow("ROLE_PROVIDER")))
                .registrationDate(LocalDateTime.now())
                .build();

        return serviceProviderMapper.entityToDto(serviceProviderRepo.save(provider));
    }

    //TODO COMPLETE METHOD
    public FinalUserDto registerAdmin(FinalUserDto dto) {

        if (dto.getPhoneNumber() != null && !dto.getPhoneNumber().isBlank()) {
            if (finalUserRepo.existsByPhoneNumber(dto.getPhoneNumber())) {
                throw new DuplicatePhoneException("The phone number is already registered");
            }
            //handlePhoneOtp(dto.getPhoneNumber());
        }

        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            if (finalUserRepo.existsByEmail(dto.getEmail()) || serviceProviderRepo.existsByEmail(dto.getEmail())) {
                throw new DuplicateEmailException("The email is already registered");
            }
            handleEmailOtp(dto.getEmail());
        }

        FinalUser user = FinalUser.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .password(passwordEncoder.encode(dto.getPassword()))
                .phoneVerified(false)
                .emailVerified(false)
                .status(UserStatus.ACTIVE)
                .roles(Set.of(roleService.getRoleOrThrow("ROLE_ADMIN")))
                .registrationDate(LocalDateTime.now())
                .build();

        return userMapper.entityToDto(finalUserRepo.save(user));
    }

    private void handleEmailOtp(String email) {
        String otp = otpService.generateOTP();
        otpService.sendOTP(email, otp);
        otpService.storeOTP(email, otp);
    }

    //TODO Complete method
    private void handlePhoneOtp(String phone) {}

    public void verifyEmail(String email, String otp) {
        if (!otpService.validateOTP(email, otp)) {
            throw new RuntimeException("OTP not valid");
        }
        boolean userFound = finalUserRepo.findByEmail(email)
                .map(user -> {
                    user.setEmailVerified(true);
                    finalUserRepo.save(user);
                    return true;
                }).orElse(false);

        boolean providerFound = serviceProviderRepo.findByEmail(email)
                .map(provider -> {
                    provider.setEmailVerified(true);
                    serviceProviderRepo.save(provider);
                    return true;
                }).orElse(false);

        if (!userFound && !providerFound) {
            throw new RuntimeException("The email provided is not registered in our system.");
        }
    }

    public ResponseEntity<AuthResponseDto> login(AuthRequestDto authRequestDto) {
        String identifier = authRequestDto.getIdentifier();

        if (finalUserRepo.existsByEmail(identifier) || finalUserRepo.existsByPhoneNumber(identifier)) {
            FinalUser user = getUser(identifier);
            if (!user.isEmailVerified()) throw new EmailNotVerifiedException("The email is not verified");
            return authenticateAndRespond(user.getEmail(), authRequestDto.getPassword());
        }

        if (serviceProviderRepo.existsByEmail(identifier) || serviceProviderRepo.existsByPhoneNumber(identifier)) {
            ServiceProvider provider = getProvider(identifier);
            return authenticateAndRespond(provider.getEmail(), authRequestDto.getPassword());
        }

        throw new UsernameNotFoundException("User not found");
    }

    public void passwordRecovery(String email) {
        if (!finalUserRepo.existsByEmail(email) && !serviceProviderRepo.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorMessage.EM_ENTITY_NOT_FOUND);
        }
        handleEmailOtp(email);
    }

    public void resetPassword(String email, String password) {
        if (!otpService.isOtpVerified(email)) {
            throw new RuntimeException("OTP not verified");
        }

        otpService.clearOtpVerification(email);
        otpService.restartResendAttempts(email);

        if (finalUserRepo.existsByEmail(email)) {
            FinalUser user = getUser(email);
            user.setPassword(passwordEncoder.encode(password));
            finalUserRepo.save(user);
        } else if (serviceProviderRepo.existsByEmail(email)) {
            ServiceProvider provider = getProvider(email);
            provider.setPassword(passwordEncoder.encode(password));
            serviceProviderRepo.save(provider);
        }
    }

    public void changePassword(String email, ChangePasswordDto changePasswordDto) {

        String currentPassword = changePasswordDto.getCurrentPassword();
        String newPassword = changePasswordDto.getNewPassword();
        String newPasswordRepeated = changePasswordDto.getNewPasswordRepeated();

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        if (!passwordEncoder.matches(currentPassword, userDetails.getPassword())) {
            throw new IllegalArgumentException("The current password is not correct");
        }

        if (!Objects.equals(newPassword, newPasswordRepeated)) {
            throw new IllegalArgumentException("The new passwords dont match");
        }

        if (finalUserRepo.existsByEmail(email)) {
            FinalUser user = getUser(email);
            user.setPassword(passwordEncoder.encode(newPassword));
            finalUserRepo.save(user);
        } else if (serviceProviderRepo.existsByEmail(email)) {
            ServiceProvider provider = getProvider(email);
            provider.setPassword(passwordEncoder.encode(newPassword));
            serviceProviderRepo.save(provider);
        } else {
            throw new RuntimeException("User not found");
        }
    }

    public void verifyOtpForPasswordReset(String email, String otp) {
        if (!otpService.validateOTP(email, otp)) {
            throw new RuntimeException("OTP not valid");
        }

        if (!finalUserRepo.existsByEmail(email) && !serviceProviderRepo.existsByEmail(email)) {
            throw new RuntimeException("Email not registered");
        }

        otpService.markOtpVerified(email);
    }


    private FinalUser getUser(String identifier) {
        return identifier.contains("@")
                ? finalUserRepo.findByEmail(identifier).orElseThrow(() -> new UsernameNotFoundException("Email not found"))
                : finalUserRepo.findByPhoneNumber(identifier).orElseThrow(() -> new UsernameNotFoundException("Phone number not found"));
    }

    private ServiceProvider getProvider(String identifier) {
        return identifier.contains("@")
                ? serviceProviderRepo.findByEmail(identifier).orElseThrow(() -> new UsernameNotFoundException("Email not found"))
                : serviceProviderRepo.findByPhoneNumber(identifier).orElseThrow(() -> new UsernameNotFoundException("Phone number not found"));
    }

    private ResponseEntity<AuthResponseDto> authenticateAndRespond(String username, String password) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
        String jwt = jwtUtil.generateToken(auth);
        return ResponseEntity.ok(new AuthResponseDto(username, jwt));
    }
}