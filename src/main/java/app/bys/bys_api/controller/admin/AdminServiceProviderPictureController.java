package app.bys.bys_api.controller.admin;

import app.bys.bys_api.mapper.ServiceProviderMapper;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequestMapping("/admin/picture/service_provider")
public class AdminServiceProviderPictureController {

    private final PictureService pictureService;
    private final ServiceProviderRepository serviceProviderRepository;
    private final ServiceProviderMapper providerMapper;
    private final PictureRepository pictureRepository;

    @PostMapping("/profile/{providerId}")
    public ResponseEntity<ServiceProviderWithPictureDto> uploadProfilePictureAsAdmin(@PathVariable Long providerId, @RequestParam("image") MultipartFile image) {
        ServiceProvider provider = serviceProviderRepository.findById(providerId)
                .orElseThrow(() -> new EntityNotFoundException("ServiceProvider with id " + providerId + " not found"));

        pictureService.uploadProfilePictureForProvider(image, provider);
        serviceProviderRepository.save(provider);

        ServiceProviderWithPictureDto providerWithPictureDto = providerMapper.entityToDtoWithPicture(provider);
        return ResponseEntity.status(HttpStatus.CREATED).body(providerWithPictureDto);

    }

    @PostMapping("/work/{providerId}")
    public ResponseEntity<ServiceProviderWithPictureDto> uploadWorkPicturesAsAdmin(@PathVariable Long providerId, @RequestParam("images") MultipartFile[] images) {
        ServiceProvider provider = serviceProviderRepository.findById(providerId)
                .orElseThrow(() -> new EntityNotFoundException("ServiceProvider not found"));

        pictureService.uploadWorkPictures(images, provider);
        serviceProviderRepository.save(provider);

        ServiceProviderWithPictureDto providerWithPictureDto = providerMapper.entityToDtoWithPicture(provider);
        return ResponseEntity.status(HttpStatus.CREATED).body(providerWithPictureDto);

    }

    @DeleteMapping("/profile/{providerId}")
    public ResponseEntity<Void> deleteProfilePictureAsAdmin(@PathVariable Long providerId) {
        ServiceProvider serviceProvider = serviceProviderRepository.findById(providerId)
                .orElseThrow(() -> new EntityNotFoundException("Service Provider with id: " + providerId + " not found"));

        Picture picture = pictureRepository.findProfilePictureByServiceProviderId(serviceProvider.getId())
                .filter(p -> p.getPictureType() == PictureType.PROFILE)
                .orElseThrow(() -> new EntityNotFoundException("Service Provider with id: " + providerId + " does not have a profile picture"));
        Long pictureId = picture.getId();

        pictureService.deletePicture(pictureId);

        serviceProvider.setProfilePicture(null);
        serviceProviderRepository.save(serviceProvider);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/work/{providerId}")
    public ResponseEntity<Void> deleteAllWorkPicturesAsAdmin(@PathVariable Long providerId) {
        ServiceProvider provider = serviceProviderRepository.findById(providerId)
                .orElseThrow(() -> new EntityNotFoundException("Service Provider with id: " + providerId + " not found"));

        pictureService.deleteAllWorkPictures(provider);
        serviceProviderRepository.save(provider);

        return ResponseEntity.noContent().build();
    }
}
