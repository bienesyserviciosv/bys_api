package app.bys.bys_api.service;

import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.model.entity.Picture;
import app.bys.bys_api.repository.MediaRepository;
import app.bys.bys_api.repository.PictureRepository;
import app.bys.bys_api.utils.MediaConstants;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PictureService {

    private final PictureRepository pictureRepository;
    private final MediaRepository mediaRepository;

    @Transactional
    public String uploadForFinalUser(MultipartFile profilePicture, FinalUser user) {
        String imageName = MediaConstants.USER_FOLDER + UUID.randomUUID();
        try {
            mediaRepository.saveImage(imageName, profilePicture);

            pictureRepository.deleteByFinalUser(user);

            Picture picture = Picture.builder()
                    .finalUser(user)
                    .url(imageName)
                    .build();
            pictureRepository.save(picture);
            return imageName;
        } catch (IOException e) {
            throw new RuntimeException("Error uploading image: " + e.getMessage());
        }
    }

    @Transactional
    public void deletePicture(Long pictureId) {
        Picture picture = pictureRepository.findById(pictureId)
                .orElseThrow(() -> new EntityNotFoundException("Picture not found"));
        mediaRepository.deleteImage(picture.getUrl());
        pictureRepository.delete(picture);
    }

}
