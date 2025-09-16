package app.bys.bys_api.controller;

import app.bys.bys_api.error.InvalidPictureException;
import app.bys.bys_api.mapper.ServiceProviderMapper;
import app.bys.bys_api.model.dto.PictureDto;
import app.bys.bys_api.model.dto.ServiceProviderWithPictureDto;
import app.bys.bys_api.model.entity.Picture;
import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.model.enums.PictureType;
import app.bys.bys_api.repository.PictureRepository;
import app.bys.bys_api.repository.ServiceProviderRepository;
import app.bys.bys_api.service.PictureService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_PROVIDER')")
@RequestMapping("/picture/service_provider")
public class ServiceProviderPictureController {

    private final PictureService pictureService;
    private final ServiceProviderRepository serviceProviderRepository;
    private final ServiceProviderMapper providerMapper;
    private final PictureRepository pictureRepository;

    @PostMapping("/profile/me")
    public ResponseEntity<ServiceProviderWithPictureDto> uploadProfilePicture(Authentication authentication, @RequestParam("image") MultipartFile image) {
        String email = authentication.getName();
        ServiceProvider provider = serviceProviderRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("ServiceProvider with email " + email + " not found"));

        String newImageUrl = pictureService.uploadProfilePictureForProvider(image, provider);

        provider.setProfilePicture(newImageUrl);
        serviceProviderRepository.save(provider);

        ServiceProviderWithPictureDto providerWithPictureDto = providerMapper.entityToDtoWithPicture(provider);
        return ResponseEntity.status(HttpStatus.CREATED).body(providerWithPictureDto);

    }

    @PostMapping("/work/me")
    public ResponseEntity<ServiceProviderWithPictureDto> uploadWorkPictures(Authentication authentication, @RequestParam("images") MultipartFile[] images) {
        String email = authentication.getName();
        ServiceProvider provider = serviceProviderRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Service Provider with email: " + email + " not found"));

        pictureService.uploadWorkPictures(images, provider);
        serviceProviderRepository.save(provider);

        ServiceProviderWithPictureDto providerWithPictureDto = providerMapper.entityToDtoWithPicture(provider);
        return ResponseEntity.status(HttpStatus.CREATED).body(providerWithPictureDto);

    }

    @DeleteMapping("/profile/me")
    public ResponseEntity<Void> deleteProfilePicture(Authentication authentication) {
        String email = authentication.getName();
        ServiceProvider serviceProvider = serviceProviderRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Service Provider with email: " + email + " not found"));

        Long providerId = serviceProvider.getId();

        Picture picture = pictureRepository.findProfilePictureByServiceProviderId(serviceProvider.getId())
                .orElseThrow(() -> new EntityNotFoundException("Service Provider with id: " + providerId + " does not have a profile picture"));
        Long pictureId = picture.getId();
        pictureService.deletePicture(pictureId);

        serviceProvider.setProfilePicture(null);
        serviceProviderRepository.save(serviceProvider);

        return ResponseEntity.noContent().build();
    }

//    private ResponseEntity<Void> deleteProfilePicture(ServiceProvider serviceProvider, Long providerId) {
//        Picture picture = pictureRepository.findProfilePictureByServiceProviderId(serviceProvider.getId())
//                .orElseThrow(() -> new EntityNotFoundException("Service Provider with id: " + providerId + " does not have a profile picture"));
//        Long pictureId = picture.getId();
//        pictureService.deletePicture(pictureId);
//
//        serviceProvider.setProfilePicture(null);
//        serviceProviderRepository.save(serviceProvider);
//
//        return ResponseEntity.noContent().build();
//    }


    @DeleteMapping("/work/me")
    public ResponseEntity<Void> deleteAllWorkPictures(Authentication authentication) {
        String email = authentication.getName();
        ServiceProvider provider = serviceProviderRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Service Provider with email: " + email + " not found"));

        pictureService.deleteAllWorkPictures(provider);
        serviceProviderRepository.save(provider);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/work/me")
    public ResponseEntity<List<PictureDto>> getMyWorkPictures(Authentication authentication) {
        String email = authentication.getName();
        ServiceProvider provider = serviceProviderRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Service Provider with email: " + email + " not found"));

        return ResponseEntity.ok(pictureService.getWorkPictures(provider));
    }


    @DeleteMapping("/work/{pictureId}")
    public ResponseEntity<Void> deleteWorkPicture(Authentication authentication, @PathVariable Long pictureId)  {
        String email = authentication.getName();
        ServiceProvider provider = serviceProviderRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Service Provider with email: " + email + " not found"));

        Picture workPicture = pictureRepository.findById(pictureId)
                .orElseThrow(() -> new EntityNotFoundException("Picture with id: " + pictureId + " not found"));

        if (workPicture.getPictureType() == PictureType.WORK && workPicture.getServiceProvider().equals(provider)) {
            pictureService.deletePicture(workPicture.getId());
            provider.getWorkPictureSet().remove(workPicture);
            serviceProviderRepository.save(provider);
            return ResponseEntity.noContent().build();
        }
        throw new InvalidPictureException("The picture is not a work image or does not belong to the authenticated provider");
    }

}
