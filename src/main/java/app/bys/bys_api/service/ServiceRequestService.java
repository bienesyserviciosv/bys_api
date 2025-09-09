package app.bys.bys_api.service;

import app.bys.bys_api.mapper.FinalUserMapper;
import app.bys.bys_api.mapper.PageMapper;
import app.bys.bys_api.mapper.ServiceRequestMapper;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.dto.ServiceRequestDto;
import app.bys.bys_api.model.dto.ServiceRequestWithPictureDto;
import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.model.entity.Picture;
import app.bys.bys_api.model.entity.ServiceRequest;
import app.bys.bys_api.repository.FinalUserRepository;
import app.bys.bys_api.repository.MediaRepository;
import app.bys.bys_api.repository.PictureRepository;
import app.bys.bys_api.repository.ServiceRequestRepository;
import app.bys.bys_api.service.specification.ServiceRequestSpecification;
import app.bys.bys_api.utils.MediaConstants;
import app.bys.bys_api.utils.specification.SearchCriteria;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceRequestService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final FinalUserRepository finalUserRepository;
    private final ServiceRequestMapper requestMapper;
    private final FinalUserMapper userMapper;
    private final MediaRepository mediaRepository;
    private final PictureRepository pictureRepository;

    public ServiceRequestWithPictureDto get(Long id) {
        return requestMapper.entityToDtoWithPicture(serviceRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Service request with id: " + id + " not found")));
    }

    public PageDto<ServiceRequestWithPictureDto> getAll(Pageable pageable, String search, List<Long> specializationList, String address, List<Long> userList, List<Long> providerList) {
        Specification<ServiceRequest> specializationSpec =
                specializationList != null ? ServiceRequestSpecification.hasSpecialization(specializationList)
                        : null;

        ServiceRequestSpecification searchSpec =
                search != null ? new ServiceRequestSpecification(
                        new SearchCriteria(
                                "name",
                                "s",
                                search
                        )
                )
                        : null;

        ServiceRequestSpecification addressSpec =
                address != null ? new ServiceRequestSpecification(
                        new SearchCriteria(
                                "address",
                                ":",
                                address
                        )
                )
                        : null;

        Specification<ServiceRequest> userSpec =
                userList != null ? ServiceRequestSpecification.hasUser(userList)
                        : null;

        Specification<ServiceRequest> providerSpec =
                providerList != null ? ServiceRequestSpecification.hasProvider(providerList)
                        : null;

        List<Specification<ServiceRequest>> specList = new ArrayList<>(Arrays.asList(
                specializationSpec,
                searchSpec,
                addressSpec,
                userSpec,
                providerSpec
        ));

        return PageMapper.pageToDto(serviceRequestRepository.findAll(
                Specification.allOf(specList.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())),
                pageable).map(requestMapper::entityToDtoWithPicture));
    }

    public ServiceRequest createWithId(Long id, ServiceRequestDto serviceRequestDto, MultipartFile[] files) {
        FinalUser finalUser = finalUserRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Final user with id: " + id + " not found"));

        serviceRequestDto.setFinalUser(userMapper.entityToDto(finalUser));
        ServiceRequest serviceRequest = serviceRequestRepository.save(requestMapper.dtoToEntity(serviceRequestDto));

        if (serviceRequest.getPictureSet() == null) {
            throw new IllegalStateException("pictureSet no fue inicializado");
        }
        uploadPictureSet(files, serviceRequest);
        return serviceRequest;
    }


    public ServiceRequest create(String email, ServiceRequestDto serviceRequestDto, MultipartFile[] files) {
        FinalUser finalUser = finalUserRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Final user with email: " + email + " not found"));

        serviceRequestDto.setFinalUser(userMapper.entityToDto(finalUser));
        ServiceRequest serviceRequest = serviceRequestRepository.save(requestMapper.dtoToEntity(serviceRequestDto));

        if (serviceRequest.getPictureSet() == null) {
            throw new IllegalStateException("pictureSet no fue inicializado");
        }
        uploadPictureSet(files, serviceRequest);
        return serviceRequest;
    }

    public ServiceRequestDto update(Long id, ServiceRequestDto serviceRequestDto) {
        ServiceRequest requestFound = serviceRequestRepository.findById(id).orElseThrow();
        requestMapper.updateServiceRequestFromDto(serviceRequestDto, requestFound);
        return requestMapper.entityToDto(serviceRequestRepository.save(requestFound));
    }

    public void delete(Long id) {
        if(!serviceRequestRepository.existsById(id)) {
            throw new EntityNotFoundException("Service request with id: " + id + "not found");
        }
        serviceRequestRepository.deleteById(id);
    }

    private void uploadPictureSet(MultipartFile[] files, ServiceRequest serviceRequest) {
        if (files != null) {
            Arrays.stream(files).forEach(file -> {
                Picture picture = new Picture();
                picture.setServiceRequest(serviceRequest);
                picture.setUrl(uploadImage(file));
                serviceRequest.getPictureSet().add(picture);
                pictureRepository.save(picture);
            });
            serviceRequestRepository.save(serviceRequest);
        }
    }

    public String uploadImage(MultipartFile image) {
        if (image != null) {
            String imageName = MediaConstants.REQUEST_FOLDER + UUID.randomUUID();
            try {
                mediaRepository.saveImage(imageName, image);
                return imageName;
            } catch (IOException exception) {
                throw new RuntimeException("Error happened uploading the images: " + exception.getMessage());
            }
        }
        return null;
    }
}
