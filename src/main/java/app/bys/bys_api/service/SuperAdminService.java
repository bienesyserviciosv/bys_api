package app.bys.bys_api.service;

import app.bys.bys_api.error.DuplicateEmailException;
import app.bys.bys_api.error.DuplicatePhoneException;
import app.bys.bys_api.mapper.FinalUserMapper;
import app.bys.bys_api.mapper.PageMapper;
import app.bys.bys_api.model.dto.FinalUserDto;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.model.enums.UserStatus;
import app.bys.bys_api.repository.FinalUserRepository;
import app.bys.bys_api.repository.ServiceProviderRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SuperAdminService {

    private final FinalUserRepository finalUserRepository;
    private final ServiceProviderRepository serviceProviderRepository;
    private final RoleService roleService;
    private final FinalUserMapper mapper;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void createSuperAdmin() {
        if (!finalUserRepository.existsByEmail("phantomxgroup7@gmail.com")) {
            FinalUser superAdmin = FinalUser.builder()
                    .name("Super Admin")
                    .email("phantomxgroup7@gmail.com")
                    .password(passwordEncoder.encode("superadmin"))
                    .roles(Set.of(roleService.getRoleOrThrow("ROLE_SUPER_ADMIN")))
                    .registrationDate(LocalDateTime.now())
                    .emailVerified(true)
                    .phoneVerified(true)
                    .build();
            finalUserRepository.save(superAdmin);
        }
    }

    public PageDto<FinalUserDto> getAll(Pageable pageable) {
        return PageMapper.pageToDto(finalUserRepository.findByRoles_Name("ROLE_ADMIN", pageable).map(mapper::entityToDto));
    }

    public FinalUserDto getSuperAdmin() {
        return mapper.entityToDto(finalUserRepository.findSuperAdmin());
    }

    public FinalUserDto getAdmin(Long id) {
       List<FinalUser> admins = finalUserRepository.findAdmins();
       FinalUser finalUser = admins.stream()
               .filter(admin -> admin.getId().equals(id))
               .findFirst()
               .orElseThrow(() -> new EntityNotFoundException("The id: " + id + " doesn't belong to an Admin"));

       return mapper.entityToDto(finalUser);
   }

    public FinalUserDto create(FinalUserDto finalUserDto) {
        if (finalUserDto.getEmail() != null && !finalUserDto.getEmail().isBlank()) {
            if (finalUserRepository.existsByEmail(finalUserDto.getEmail()) || serviceProviderRepository.existsByEmail(finalUserDto.getEmail())) {
                throw new DuplicateEmailException("The email is already registered");
            }
        }
        if (finalUserDto.getPhoneNumber() != null && !finalUserDto.getPhoneNumber().isBlank()) {
            if (finalUserRepository.existsByPhoneNumber(finalUserDto.getPhoneNumber())) {
                throw new DuplicatePhoneException("The phone number is already registered");
            }
        }
        FinalUser admin = FinalUser.builder()
                .name(finalUserDto.getName())
                .email(finalUserDto.getEmail())
                .phoneNumber(finalUserDto.getPhoneNumber())
                .phoneVerified(true)
                .emailVerified(true)
                .password(passwordEncoder.encode(finalUserDto.getPassword()))
                .status(UserStatus.ACTIVE)
                .roles(Set.of(roleService.getRoleOrThrow("ROLE_ADMIN")))
                .registrationDate(LocalDateTime.now())
                .build();
        log.info("Admin created with email: {}", finalUserDto.getEmail());

        return mapper.entityToDto(finalUserRepository.save(admin));
    }

    public FinalUserDto update(Long id, FinalUserDto finalUserDto) {
        FinalUser userFound = finalUserRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Admin with id: " + id + " not found"));
        mapper.updateFinalUserFromDto(finalUserDto, userFound);
        return mapper.entityToDto(finalUserRepository.save(userFound));
    }

   public void delete(Long id) {
       FinalUser userFound = finalUserRepository.findById(id)
               .orElseThrow(() -> new EntityNotFoundException("Admin with id: " + id + " not found"));

       finalUserRepository.delete(userFound);
   }
}
