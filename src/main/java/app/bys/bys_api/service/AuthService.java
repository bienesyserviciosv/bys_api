package app.bys.bys_api.service;

import app.bys.bys_api.mapper.FinalUserMapper;
import app.bys.bys_api.mapper.ServiceProviderMapper;
import app.bys.bys_api.model.dto.AuthRequest;
import app.bys.bys_api.model.dto.AuthResponse;
import app.bys.bys_api.model.dto.FinalUserDto;
import app.bys.bys_api.model.dto.ServiceProviderDto;
import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.model.entity.Role;
import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.model.enums.Level;
import app.bys.bys_api.model.enums.UserStatus;
import app.bys.bys_api.repository.FinalUserRepository;
import app.bys.bys_api.repository.RoleRepository;
import app.bys.bys_api.repository.ServiceProviderRepository;
import app.bys.bys_api.utils.JwtUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final FinalUserRepository finalUserRepo;
    private final ServiceProviderRepository serviceProviderRepo;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final FinalUserMapper finalUserMapper;
    private final ServiceProviderMapper serviceProviderMapper;
    private final OtpService otpService;

    @PostConstruct  // Se ejecutará al iniciar la aplicación
    public void initRoles() {
        createRoleIfNotFound("ROLE_USER");
        createRoleIfNotFound("ROLE_PROVIDER");
        createRoleIfNotFound("ROLE_ADMIN");
    }

    private void createRoleIfNotFound(String roleName) {
        if (!roleRepository.existsByName(roleName)) {
            Role role = new Role();
            role.setName(roleName);
            roleRepository.save(role);
        }
    }

    public FinalUserDto registerFinalUser(FinalUserDto dto) {
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Rol ROLE_USER not found"));
        FinalUser user = FinalUser.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .password(passwordEncoder.encode(dto.getPassword()))
                .emailVerified(false)
                .status(UserStatus.ACTIVE)
                .roles(Set.of(userRole))
                .build();

        String otp = otpService.generateOTP();
        otpService.sendOTP(dto.getEmail(), otp);
        otpService.storeOTP(dto.getEmail(), otp);

        return finalUserMapper.entityToDto(finalUserRepo.save(user));
    }

    public ServiceProviderDto registerServiceProvider(ServiceProviderDto dto) {
        Role providerRole = roleRepository.findByName("ROLE_PROVIDER")
                .orElseThrow(() -> new RuntimeException("Rol ROLE_PROVIDER not found"));
        ServiceProvider provider = ServiceProvider.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .password(passwordEncoder.encode(dto.getPassword()))
                .status(UserStatus.ACTIVE)
                .experience(dto.getExperience())
                .emailVerified(false)
                .level(Level.NOT_VERIFIED)
                .verified(false)
                .roles(Set.of(providerRole))
                .build();

        String otp = otpService.generateOTP();
        otpService.sendOTP(dto.getEmail(), otp);
        otpService.storeOTP(dto.getEmail(), otp);

        return serviceProviderMapper.entityToDto(serviceProviderRepo.save(provider));
    }

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

        // 3. Si no existe en ningún repositorio
        if (!userFound && !providerFound) {
            throw new RuntimeException("El email proporcionado no está registrado en nuestro sistema");
        }
    }

    public ResponseEntity<AuthResponse> login(AuthRequest authRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequest.getEmail(), authRequest.getPassword()
                )
        );

        String jwt = jwtUtil.generateToken(authentication);
        AuthResponse response = AuthResponse.builder()
                .token(jwt)
                .username(authRequest.getEmail())
                .build();

        return ResponseEntity.ok(response);
    }
}
