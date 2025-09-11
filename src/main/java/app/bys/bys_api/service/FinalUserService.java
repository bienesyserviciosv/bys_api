package app.bys.bys_api.service;

import app.bys.bys_api.error.DuplicateEmailException;
import app.bys.bys_api.error.DuplicatePhoneException;
import app.bys.bys_api.mapper.FinalUserMapper;
import app.bys.bys_api.mapper.PageMapper;
import app.bys.bys_api.model.dto.FinalUserDto;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.model.entity.Picture;
import app.bys.bys_api.model.entity.Role;
import app.bys.bys_api.repository.FinalUserRepository;
import app.bys.bys_api.repository.MediaRepository;
import app.bys.bys_api.repository.PictureRepository;
import app.bys.bys_api.utils.MediaConstants;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinalUserService {

    private final FinalUserRepository finalUserRepository;
    private final FinalUserMapper mapper;
    private final RoleService roleService;
    private final PictureRepository pictureRepository;
    private final MediaRepository mediaRepository;

    public FinalUserDto get(Long id) {
        return mapper.entityToDto(finalUserRepository.findById(id).
                orElseThrow(() -> new EntityNotFoundException("Final user with id: " + id + " not found")));
    }

    public FinalUserDto getWithEmail(String email) {
        return mapper.entityToDto(finalUserRepository.findByEmail(email).
                orElseThrow(() -> new EntityNotFoundException("Final user with email: " + email + " not found")));
    }

    public PageDto<FinalUserDto> getAll(Pageable pageable) {
        return PageMapper.pageToDto(finalUserRepository.findAll(pageable).map(mapper::entityToDto));
    }

    public FinalUserDto create(FinalUserDto finalUserDto) {
        if (finalUserRepository.existsByEmail(finalUserDto.getEmail())) {
            throw new DuplicateEmailException("The email is already registered");
        }
        if (finalUserRepository.existsByPhoneNumber(finalUserDto.getPhoneNumber())) {
            throw new DuplicatePhoneException("The phone number is already registered");
        }

        FinalUser user = FinalUser.builder()
                .name(finalUserDto.getName())
                .email(finalUserDto.getEmail())
                .phoneNumber(finalUserDto.getPhoneNumber())
                .phoneVerified(false)
                .emailVerified(false)
                .roles(Set.of(roleService.getRoleOrThrow("ROLE_USER")))
                .registrationDate(LocalDateTime.now())
                .build();

        return mapper.entityToDto(finalUserRepository.save(user));
    }

    public FinalUserDto update(Long id, FinalUserDto finalUserDto) {
        FinalUser userFound = finalUserRepository.findById(id).
                orElseThrow(() -> new EntityNotFoundException("Final user with id: " + id + " not found"));
        mapper.updateFinalUserFromDto(finalUserDto, userFound);
        return mapper.entityToDto(finalUserRepository.save(userFound));
    }

    public FinalUserDto updateByEmail(String email, FinalUserDto finalUserDto) {
        FinalUser userFound = finalUserRepository.findByEmail(email).
                orElseThrow(() -> new EntityNotFoundException("Final user with email: " + email + " not found"));
        mapper.updateFinalUserFromDto(finalUserDto, userFound);
        return mapper.entityToDto(finalUserRepository.save(userFound));
    }

    public void delete(Long id) {
        if (!finalUserRepository.existsById(id)) {
            throw new EntityNotFoundException("Final user with id: " + id + " not found");
        }
        Picture picture = pictureRepository.findByFinalUserId(id).orElseThrow();
        pictureRepository.delete(picture);

        finalUserRepository.deleteById(id);
    }

    public void deleteByEmail(String email) {
        if (!finalUserRepository.existsByEmail(email)) {
            throw new EntityNotFoundException("Final user with email: " + email + " not found");
        }
        finalUserRepository.deleteByEmail(email);
    }


    public FinalUser findOrCreateUser(String email, String name) {
        log.info("Recibido email: " + email + ", nombre: " + name);
        return finalUserRepository.findByEmail(email)
                .orElseGet(() -> {
                    FinalUser newUser = new FinalUser();
                    newUser.setEmail(email);
                    newUser.setName(name);
                    newUser.setRegistrationDate(LocalDateTime.now());
                    Role userRole = roleService.getOrCreateRole("ROLE_USER");
                    newUser.setRoles(Set.of(userRole));
                    newUser.setRegistrationDate(LocalDateTime.now());
                    return finalUserRepository.save(newUser);

                });
    }

    public FinalUser findOrCreateUserFromGoogle(GoogleIdToken.Payload payload) {
        String email = payload.getEmail();
        Optional<FinalUser> existingUser = finalUserRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        FinalUser newUser = new FinalUser();
        newUser.setEmail(email);

        String name = (String) payload.get("name");
        if (name == null || name.isBlank()) {
            name = email.split("@")[0]; // o "Usuario sin nombre"
        }
        newUser.setName(name);

        Role userRole = roleService.getOrCreateRole("ROLE_USER");
        newUser.setRoles(Set.of(userRole));
        newUser.setPassword("oauth2_dummy");
        newUser.setRegistrationDate(LocalDateTime.now());
//        newUser.setPictureUrl((String) payload.get("picture"));
//        newUser.setProvider(AuthProvider.GOOGLE);
        return finalUserRepository.save(newUser);
    }

    public void uploadPictureSet(MultipartFile profilePicture, FinalUser finalUser) {
        if (profilePicture != null) {
            Picture picture = new Picture();
            picture.setFinalUser(finalUser);
            String url = uploadImage(profilePicture);
            picture.setUrl(url);
            finalUser.setProfilePicture(url);
            pictureRepository.save(picture);
        }
    }

    public String uploadImage(MultipartFile image) {
        if (image != null) {
            String imageName = MediaConstants.USER_FOLDER + UUID.randomUUID();
            try {
                mediaRepository.saveImage(imageName, image);
                return imageName;
            } catch (IOException exception) {
                throw new RuntimeException("Error happened uploading the images: " + exception.getMessage());
            }
        }
        return null;
    }
}
