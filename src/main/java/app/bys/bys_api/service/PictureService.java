package app.bys.bys_api.service;

import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.model.entity.Picture;
import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.model.enums.PictureType;
import app.bys.bys_api.repository.MediaRepository;
import app.bys.bys_api.repository.PictureRepository;
import app.bys.bys_api.utils.MediaConstants;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PictureService {

    private final PictureRepository pictureRepository;
    private final MediaRepository mediaRepository;

    @Transactional
    public String uploadProfilePictureForFinalUser(MultipartFile profilePicture, FinalUser user) {
        String imageName = MediaConstants.USER_FOLDER + UUID.randomUUID();
        try {
            mediaRepository.saveImage(imageName, profilePicture);

            pictureRepository.deleteByFinalUser(user);

            Picture picture = Picture.builder()
                    .finalUser(user)
                    .url(imageName)
                    .pictureType(PictureType.PROFILE)
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

    @Transactional
    public String uploadProfilePictureForProvider(MultipartFile image, ServiceProvider provider) {
        String oldImage = provider.getProfilePicture();
        if (oldImage != null) {
            mediaRepository.deleteImage(oldImage);
            pictureRepository.deleteByServiceProviderAndUrl(provider, oldImage);
        }

        String imageName = MediaConstants.PROVIDER_FOLDER + UUID.randomUUID();


        try {
            mediaRepository.saveImage(imageName, image);
            Picture picture = Picture.builder()
                    .serviceProvider(provider)
                    .url(imageName)
                    .pictureType(PictureType.PROFILE)
                    .build();
            pictureRepository.save(picture);

            return imageName;
        } catch (IOException e) {
            throw new RuntimeException("Error happened uploading the images: " + e.getMessage());
        }
    }

    @Transactional
    public void uploadWorkPictures(MultipartFile[] workPictureList, ServiceProvider provider) {
        Set<Picture> oldPictures = provider.getWorkPictureSet();

        oldPictures.forEach(picture -> {
            mediaRepository.deleteImage(picture.getUrl());
            pictureRepository.delete(picture);
        });
        provider.getWorkPictureSet().clear();

        Arrays.stream(workPictureList).forEach(image -> {
            String imageName = MediaConstants.PROVIDER_FOLDER + UUID.randomUUID();
            try {
                mediaRepository.saveImage(imageName, image);
                Picture picture = Picture.builder()
                        .serviceProvider(provider)
                        .url(imageName)
                        .pictureType(PictureType.WORK)
                        .build();
                pictureRepository.save(picture);
                provider.getWorkPictureSet().add(picture);

            } catch (IOException e) {
                throw new RuntimeException("Error happened uploading the images: " + e.getMessage());
            }

        });
    }

    @Transactional
    public void deleteAllWorkPictures(ServiceProvider provider) {
        Set<Picture> workPictures = provider.getWorkPictureSet();

        if (workPictures.isEmpty()) {
            return;
        }

        workPictures.forEach(picture -> {
            mediaRepository.deleteImage(picture.getUrl());
            pictureRepository.delete(picture);
        });

        provider.getWorkPictureSet().clear();
    }


}
