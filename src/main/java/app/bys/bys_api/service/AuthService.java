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
                .orElseThrow(() -> new RuntimeException("Rol ROLE_USER no encontrado"));
        FinalUser user = FinalUser.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .password(passwordEncoder.encode(dto.getPassword()))
                .status(UserStatus.ACTIVE)
                .roles(Set.of(userRole))
                //.roles(Set.of(Role.builder().name("ROLE_USER").build()))
                //.roles(List.of(roleRepository.findByName("ROLE_USER")))
                .build();

        return finalUserMapper.entityToDto(finalUserRepo.save(user));
    }

    public ServiceProviderDto registerServiceProvider(ServiceProviderDto dto) {
        Role providerRole = roleRepository.findByName("ROLE_PROVIDER")
                .orElseThrow(() -> new RuntimeException("Rol ROLE_PROVIDER no encontrado"));
        ServiceProvider provider = ServiceProvider.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .status(UserStatus.ACTIVE)
                .experience(dto.getExperience())
                .level(Level.NOT_VERIFIED)
                .verified(false)
                .roles(Set.of(providerRole))
                .password(passwordEncoder.encode(dto.getPassword()))
                .build();


        return serviceProviderMapper.entityToDto(serviceProviderRepo.save(provider));
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
