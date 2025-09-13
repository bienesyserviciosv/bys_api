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

    @PostMapping("/final_user/{userId}")
    public ResponseEntity<FinalUserDto> uploadPicture(@PathVariable Long userId, @RequestParam("image") MultipartFile newImage) {
        FinalUser finalUser = finalUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Final user with id: " + userId + " not found"));

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

    @DeleteMapping("/final_user/{userId}")
    public ResponseEntity<Void> deletePicture(@PathVariable Long userId) {
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
