package app.bys.bys_api.controller;

import app.bys.bys_api.mapper.FinalUserMapper;
import app.bys.bys_api.model.dto.FinalUserDto;
import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.model.entity.Picture;
import app.bys.bys_api.repository.FinalUserRepository;
import app.bys.bys_api.repository.MediaRepository;
import app.bys.bys_api.repository.PictureRepository;
import app.bys.bys_api.service.PictureService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_USER')")
@RequestMapping("/picture/final_user")
public class FinalUserPictureController {

    private final PictureService pictureService;
    private final FinalUserRepository finalUserRepository;
    private final FinalUserMapper finalUserMapper;
    private final MediaRepository mediaRepository;
    private final PictureRepository pictureRepository;

    @PostMapping("/profile/me")
    public ResponseEntity<FinalUserDto> uploadPicture(Authentication authentication, @RequestParam("image") MultipartFile newImage) {
        String email = authentication.getName();
        FinalUser finalUser = finalUserRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Final user with email: " + email + " not found"));

        String oldImage = finalUser.getProfilePicture();
        if (oldImage != null) {
            mediaRepository.deleteImage(oldImage);
        }
        String newImageUrl = pictureService.uploadProfilePictureForFinalUser(newImage, finalUser);
        finalUser.setProfilePicture(newImageUrl);
        finalUserRepository.save(finalUser);
        FinalUserDto finalUserDto = finalUserMapper.entityToDto(finalUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(finalUserDto);
    }

    @DeleteMapping("/profile/me")
    public ResponseEntity<Void> deletePicture(Authentication authentication) {
        String email = authentication.getName();
        FinalUser finalUser = finalUserRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Final user with email: " + email + " not found"));

        Long userId = finalUser.getId();

        Picture picture = pictureRepository.findByFinalUserId(finalUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("Final user with id: " + userId + " does not have a picture"));
        Long pictureId = picture.getId();
        pictureService.deletePicture(pictureId);

        finalUser.setProfilePicture(null);
        finalUserRepository.save(finalUser);

        return ResponseEntity.noContent().build();
    }
}
