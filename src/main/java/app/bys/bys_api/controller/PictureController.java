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
@RequestMapping("/picture")
@RequiredArgsConstructor
public class PictureController {

    private final PictureService pictureService;
    private final FinalUserRepository finalUserRepository;
    private final FinalUserMapper finalUserMapper;
    private final MediaRepository mediaRepository;
    private final PictureRepository pictureRepository;

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/final_user/{userId}")
    public ResponseEntity<FinalUserDto> uploadPictureAsAdmin(@PathVariable Long userId, @RequestParam("image") MultipartFile newImage) {
        FinalUser finalUser = finalUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Final user with id: " + userId + " not found"));

        return updateProfilePicture(newImage, finalUser);
    }

    @PreAuthorize("hasAuthority('ROLE_USER')")
    @PostMapping("/final_user/me")
    public ResponseEntity<FinalUserDto> uploadPicture(Authentication authentication, @RequestParam("image") MultipartFile newImage) {
        String email = authentication.getName();
        FinalUser finalUser = finalUserRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Final user with email: " + email + " not found"));

        return updateProfilePicture(newImage, finalUser);
    }

    private ResponseEntity<FinalUserDto> updateProfilePicture(MultipartFile newImage, FinalUser finalUser) {
        String oldImage = finalUser.getProfilePicture();
        if (oldImage != null) {
            mediaRepository.deleteImage(oldImage);
        }
        String newImageUrl = pictureService.uploadForFinalUser(newImage, finalUser);
        finalUser.setProfilePicture(newImageUrl);
        finalUserRepository.save(finalUser);
        FinalUserDto finalUserDto = finalUserMapper.entityToDto(finalUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(finalUserDto);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/final_user/{userId}")
    public ResponseEntity<Void> deletePictureAsAdmin(@PathVariable Long userId) {
        FinalUser finalUser = finalUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Final user with id: " + userId + " not found"));

        return deleteProfilePicture(finalUser, userId);
    }

    @PreAuthorize("hasAuthority('ROLE_USER')")
    @DeleteMapping("/final_user/me")
    public ResponseEntity<Void> deletePicture(Authentication authentication) {
        String email = authentication.getName();
        FinalUser finalUser = finalUserRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Final user with email: " + email + " not found"));

        Long userId = finalUser.getId();

        return deleteProfilePicture(finalUser, userId);
    }

    private ResponseEntity<Void> deleteProfilePicture(FinalUser finalUser, Long userId) {
        Picture picture = pictureRepository.findByFinalUserId(finalUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("Final user with id: " + userId + " does not have a picture"));
        Long pictureId = picture.getId();
        pictureService.deletePicture(pictureId);

        finalUser.setProfilePicture(null);
        finalUserRepository.save(finalUser);

        return ResponseEntity.noContent().build();
    }


}
