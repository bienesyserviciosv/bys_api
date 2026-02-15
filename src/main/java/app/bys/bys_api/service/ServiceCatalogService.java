package app.bys.bys_api.service;

import app.bys.bys_api.mapper.PageMapper;
import app.bys.bys_api.mapper.ServiceCatalogMapper;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.dto.ServiceCatalogDto;
import app.bys.bys_api.model.entity.Picture;
import app.bys.bys_api.model.entity.ServiceCatalog;
import app.bys.bys_api.model.enums.PictureType;
import app.bys.bys_api.repository.MediaRepository;
import app.bys.bys_api.repository.PictureRepository;
import app.bys.bys_api.repository.ServiceCatalogRepository;
import app.bys.bys_api.utils.MediaConstants;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceCatalogService {

    private final ServiceCatalogRepository serviceCatalogRepository;
    private final ServiceCatalogMapper mapper;
    private final MediaRepository mediaRepository;
    private final PictureRepository pictureRepository;
    private final PictureService pictureService;

    @Value("${media.url}")
    public String mediaUrl;

    public ServiceCatalogDto get(Long id) {
        ServiceCatalogDto dto = serviceCatalogRepository.findCatalogById(id)
                .orElseThrow(() -> new EntityNotFoundException("Service catalog with id " + id + " not found"));

        List<String> pictures = pictureRepository.findPicturesByCatalogId(id);

        dto.setServicePictures(
                pictures.stream()
                        .map(url -> mediaUrl + url)
                        .collect(Collectors.toSet()) );

        return dto;
    }

    public PageDto<ServiceCatalogDto> getAll(String search, String description, List<Long> specializationIdList, Pageable pageable) {

        if (search == null) search = "" ;
        if (description == null) description = "";

        Page<ServiceCatalogDto> page = serviceCatalogRepository.findAllCatalogs(
                search,
                description,
                specializationIdList,
                pageable
        );

        Map<Long, Set<String>> picturesByCatalog = pictureRepository.findCatalogIdAndPictureUrlList()
                .stream()
                .filter(row -> row[0] != null)
                .collect(Collectors.groupingBy(
                        row -> (Long) row[0],
                        Collectors.mapping(row -> (String) row[1], Collectors.toSet())
                ));

        page.forEach(dto -> {
            Set<String> urls = picturesByCatalog.getOrDefault(dto.getId(), Set.of());
            dto.setServicePictures( urls.stream()
                    .map(url -> mediaUrl + url)
                    .collect(Collectors.toSet())
            );
        });

        return PageMapper.pageToDto(page);

    }

    @Transactional
    public ServiceCatalogDto create(ServiceCatalogDto dto, MultipartFile[] files) {
        ServiceCatalog serviceCatalog = mapper.dtoToEntity(dto);

        if (files != null && files.length > 0) {
            uploadPictureSet(files, serviceCatalog);
        }

        ServiceCatalog saved = serviceCatalogRepository.save(serviceCatalog);
        return mapper.entityToDto(saved);
    }

    @Transactional
    public ServiceCatalogDto update(Long id, ServiceCatalogDto dto, MultipartFile[] files) {
        ServiceCatalog serviceCatalogStored = serviceCatalogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Service catalog with id " + id + " not found"));

        if (files != null && files.length > 0) {
            pictureService.deleteAllServiceCatalogPictures(serviceCatalogStored);
            uploadPictureSet(files, serviceCatalogStored);
        }
        mapper.updateServiceCatalogFromDto(dto, serviceCatalogStored);
        ServiceCatalog saved = serviceCatalogRepository.save(serviceCatalogStored);
        return mapper.entityToDto(saved);
    }

    private void uploadPictureSet(MultipartFile[] files, ServiceCatalog serviceCatalog) {
        Arrays.stream(files)
                .filter(file -> file != null && !file.isEmpty())
                .forEach(file -> {
                    Picture picture = new Picture();
                    picture.setServiceCatalog(serviceCatalog);
                    picture.setPictureType(PictureType.SERVICE_CATALOG);
                    picture.setUrl(uploadImage(file));
                    pictureRepository.save(picture);
                    serviceCatalog.getServicePictures().add(picture);
                });
    }

    private String uploadImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Image file is empty or null");
        }
            String imageName = MediaConstants.SERVICE_CATALOG_FOLDER + UUID.randomUUID();
            try {
                mediaRepository.saveImage(imageName, image);
                return imageName;
            } catch (IOException exception) {
                throw new RuntimeException("Error happened uploading the images: " + exception.getMessage());
            }
        }

    public void delete(Long id) {
        ServiceCatalog serviceCatalog = serviceCatalogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Service catalog with id " + id + " not found"));
        pictureService.deleteAllServiceCatalogPictures(serviceCatalog);
        serviceCatalogRepository.deleteById(id);
    }
}
