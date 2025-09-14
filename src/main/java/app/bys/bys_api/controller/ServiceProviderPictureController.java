package app.bys.bys_api.controller;

import app.bys.bys_api.mapper.ServiceProviderMapper;
import app.bys.bys_api.model.dto.ServiceProviderWithPictureDto;
import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.repository.MediaRepository;
import app.bys.bys_api.repository.PictureRepository;
import app.bys.bys_api.repository.ServiceProviderRepository;
import app.bys.bys_api.service.PictureService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/picture/service_provider")
public class ServiceProviderPictureController {

    private final PictureService pictureService;
    private final ServiceProviderRepository serviceProviderRepository;
    private final ServiceProviderMapper providerMapper;
    private final ServiceProviderMapper serviceProviderMapper;
    private final MediaRepository mediaRepository;
    private final PictureRepository pictureRepository;

    @PostMapping("/profile/{providerId}")
    public ResponseEntity<ServiceProviderWithPictureDto> uploadProfilePicture(@PathVariable Long providerId, @RequestParam("image") MultipartFile image) {
        ServiceProvider provider = serviceProviderRepository.findById(providerId)
                .orElseThrow(() -> new EntityNotFoundException("ServiceProvider not found"));

        String newImageUrl = pictureService.uploadProfilePictureForProvider(image, provider);

        provider.setProfilePicture(newImageUrl);
        serviceProviderRepository.save(provider);

        ServiceProviderWithPictureDto providerWithPictureDto = providerMapper.entityToDtoWithPicture(provider);
        return ResponseEntity.status(HttpStatus.CREATED).body(providerWithPictureDto);

    }

    @PostMapping("/work/{providerId}")
    public ResponseEntity<ServiceProviderWithPictureDto> uploadWorkPictures(@PathVariable Long providerId, @RequestParam("images") MultipartFile[] images) {
        ServiceProvider provider = serviceProviderRepository.findById(providerId)
                .orElseThrow(() -> new EntityNotFoundException("ServiceProvider not found"));

        pictureService.uploadWorkPictures(images, provider);
        serviceProviderRepository.save(provider);

        ServiceProviderWithPictureDto providerWithPictureDto = providerMapper.entityToDtoWithPicture(provider);
        return ResponseEntity.status(HttpStatus.CREATED).body(providerWithPictureDto);

    }


}
