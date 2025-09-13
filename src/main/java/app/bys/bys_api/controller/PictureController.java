package app.bys.bys_api.controller;

import app.bys.bys_api.mapper.FinalUserMapper;
import app.bys.bys_api.model.dto.FinalUserDto;
import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.repository.FinalUserRepository;
import app.bys.bys_api.repository.MediaRepository;
import app.bys.bys_api.service.PictureService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/final_user/{userId}")
    public ResponseEntity<FinalUserDto> uploadPicture(@PathVariable Long userId, @RequestParam("image") MultipartFile newImage) {
        FinalUser finalUser = finalUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Final user with email: " + userId + " not found"));

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

    @DeleteMapping("/final_user/{pictureId}")
    public ResponseEntity<Void> deletePicture(@PathVariable Long pictureId) {
        pictureService.deletePicture(pictureId);
        return ResponseEntity.noContent().build();
    }
}
