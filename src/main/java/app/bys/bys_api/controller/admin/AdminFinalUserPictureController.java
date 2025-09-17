package app.bys.bys_api.controller.admin;

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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequestMapping("/admin/picture/final_user")
public class AdminFinalUserPictureController {

    private final PictureService pictureService;
    private final FinalUserRepository finalUserRepository;
    private final FinalUserMapper finalUserMapper;
    private final MediaRepository mediaRepository;
    private final PictureRepository pictureRepository;


    @PostMapping("/profile/{userId}")
    public ResponseEntity<FinalUserDto> uploadPictureAsAdmin(@PathVariable Long userId, @RequestParam("image") MultipartFile newImage) {
        FinalUser finalUser = finalUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Final user with id: " + userId + " not found"));

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

    @DeleteMapping("/profile/{userId}")
    public ResponseEntity<Void> deletePictureAsAdmin(@PathVariable Long userId) {
        FinalUser finalUser = finalUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Final user with id: " + userId + " not found"));

        Picture picture = pictureRepository.findByFinalUserId(finalUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("Final user with id: " + userId + " does not have a picture"));
        Long pictureId = picture.getId();
        pictureService.deletePicture(pictureId);

        finalUser.setProfilePicture(null);
        finalUserRepository.save(finalUser);

        return ResponseEntity.noContent().build();
    }
}
