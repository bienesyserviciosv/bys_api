package app.bys.bys_api.service;

import app.bys.bys_api.mapper.PictureMapper;
import app.bys.bys_api.model.dto.PictureDto;
import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.model.entity.Picture;
import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.model.enums.PictureType;
import app.bys.bys_api.repository.MediaRepository;
import app.bys.bys_api.repository.PictureRepository;
import app.bys.bys_api.utils.MediaConstants;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PictureService {

    private final PictureRepository pictureRepository;
    private final MediaRepository mediaRepository;
    private final PictureMapper pictureMapper;

    @Transactional
    public void uploadProfilePictureForFinalUser(MultipartFile profilePicture, FinalUser user) {
        if (profilePicture != null && !profilePicture.isEmpty()) {

            String oldImage = user.getProfilePicture();
            if (oldImage != null) {
                mediaRepository.deleteImage(oldImage);
                pictureRepository.deleteByFinalUser(user);
            }

            String imageName = MediaConstants.USER_FOLDER + UUID.randomUUID();

            try {
                mediaRepository.saveImage(imageName, profilePicture);
                Picture picture = Picture.builder()
                        .finalUser(user)
                        .url(imageName)
                        .pictureType(PictureType.PROFILE)
                        .build();
                user.setProfilePicture(imageName);
                pictureRepository.save(picture);

            } catch (IOException e) {
                throw new RuntimeException("Error uploading image: " + e.getMessage());
            }
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
    public void uploadProfilePictureForProvider(MultipartFile image, ServiceProvider provider) {
        if (image != null && !image.isEmpty()) {
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
                provider.setProfilePicture(imageName);
                pictureRepository.save(picture);

            } catch (IOException e) {
                throw new RuntimeException("Error happened uploading the images: " + e.getMessage());
            }
        }
    }

    @Transactional
    public void uploadWorkPictures(MultipartFile[] workPictureList, ServiceProvider provider) {
        if (workPictureList != null) {
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

    @Transactional
    public void deleteAllServiceCatalogPicturesByCatalogId(Long catalogId) {

        List<String> urls = pictureRepository.findUrlsByCatalogId(catalogId);

        urls.forEach(url -> {
            try {
                mediaRepository.deleteImage(url);
            } catch (Exception e) {
                log.error("Failed to delete image: {}", url, e);
            }
        });

        pictureRepository.deleteAllByCatalogId(catalogId);
    }

    @Transactional
    public List<PictureDto> getWorkPictures(ServiceProvider provider) {
        List<Picture> pictures = pictureRepository.findByServiceProviderAndPictureType(provider, PictureType.WORK);

        return pictures.stream()
                .map(pictureMapper::entityToDto)
                .toList();
    }

}
