package app.bys.bys_api.service;

import app.bys.bys_api.error.DuplicateEmailException;
import app.bys.bys_api.error.DuplicatePhoneException;
import app.bys.bys_api.mapper.PageMapper;
import app.bys.bys_api.mapper.ServiceProviderMapper;
import app.bys.bys_api.mapper.SpecializationMapper;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.dto.ServiceProviderDto;
import app.bys.bys_api.model.entity.Picture;
import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.repository.MediaRepository;
import app.bys.bys_api.repository.PictureRepository;
import app.bys.bys_api.model.enums.MembershipType;
import app.bys.bys_api.repository.ServiceProviderRepository;
import app.bys.bys_api.service.specification.ServiceProviderSpecification;
import app.bys.bys_api.utils.MediaConstants;
import app.bys.bys_api.utils.specification.SearchCriteria;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceProviderService {

    private final ServiceProviderRepository serviceProviderRepository;
    private final ServiceProviderMapper mapper;
    private final RoleService roleService;
    private final SpecializationMapper specializationMapper;
    private final MediaRepository mediaRepository;
    private final PictureRepository pictureRepository;

    public ServiceProviderDto get(Long id) {
        return mapper.entityToDto(serviceProviderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Service provider with id " + id + " not found")));
    }

    public ServiceProviderDto getWithEmail(String email) {
        return mapper.entityToDto(serviceProviderRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Service provider with email " + email+ " not found")));
    }

    public PageDto<ServiceProviderDto> getAll(Pageable pageable, String search, List<Long> specializationList, String address) {
        Specification<ServiceProvider> specializationSpec =
                specializationList != null ? ServiceProviderSpecification.hasSpecialization(specializationList)
                        : null;

        ServiceProviderSpecification searchSpec =
                search != null ? new ServiceProviderSpecification(
                        new SearchCriteria(
                                "name",
                                "s",
                                search
                        )
                )
                        : null;

        ServiceProviderSpecification addressSpec =
                address != null ? new ServiceProviderSpecification(
                        new SearchCriteria(
                                "address",
                                ":",
                                address
                        )
                )
                        : null;

        List<Specification<ServiceProvider>> specList = new ArrayList<>(Arrays.asList(
                specializationSpec,
                searchSpec,
                addressSpec
        ));

        return PageMapper.pageToDto(serviceProviderRepository.findAll(
                Specification.allOf(specList.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList())),
                pageable).map(mapper::entityToDto));
    }

    public ServiceProviderDto create(ServiceProviderDto serviceProviderDto, MultipartFile profilePicture, MultipartFile[] workPictureSet) {
        if (serviceProviderRepository.existsByEmail(serviceProviderDto.getEmail())) {
            throw new DuplicateEmailException("The email is already registered");
        }

        if (serviceProviderRepository.existsByPhoneNumber(serviceProviderDto.getPhoneNumber())) {
            throw new DuplicatePhoneException("The phone is already registered");
        }


        ServiceProvider provider = ServiceProvider.builder()
                .name(serviceProviderDto.getName())
                .email(serviceProviderDto.getEmail())
                .phoneNumber(serviceProviderDto.getPhoneNumber())
                .experience(serviceProviderDto.getExperience())
                .specializations(specializationMapper.setDtoToEntitySet(serviceProviderDto.getSpecializations()))
                .emailVerified(false)
                .phoneVerified(false)
                .membershipType(MembershipType.NOT_VERIFIED)
                .verified(false)
                .address(serviceProviderDto.getAddress())
                .roles(Set.of(roleService.getRoleOrThrow("ROLE_PROVIDER")))
                .registrationDate(LocalDateTime.now())
                .build();

        ServiceProvider providerSaved = serviceProviderRepository.save(provider);
        uploadPictureSet(workPictureSet, profilePicture, providerSaved);
        return mapper.entityToDto(providerSaved);
    }

    public ServiceProviderDto update(Long id, ServiceProviderDto serviceProviderDto) {
        ServiceProvider serviceProviderFound = serviceProviderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Service provider with id " + id + " not found"));

        mapper.updateServiceProviderFromDto(serviceProviderDto, serviceProviderFound);
        return mapper.entityToDto(serviceProviderRepository.save(serviceProviderFound));
    }

    public ServiceProviderDto updateByEmail(String email, ServiceProviderDto serviceProviderDto) {
        ServiceProvider serviceProviderFound = serviceProviderRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Service provider with email " + email + " not found"));

        mapper.updateServiceProviderFromDto(serviceProviderDto, serviceProviderFound);
        return mapper.entityToDto(serviceProviderRepository.save(serviceProviderFound));
    }

    public void delete(Long id) {
        if (!serviceProviderRepository.existsById(id)) {
            throw new EntityNotFoundException("Service provider with id " + id + " not found");
        }
        serviceProviderRepository.deleteById(id);
    }

    public void deleteByEmail(String email) {
        if (!serviceProviderRepository.existsByEmail(email)) {
            throw new EntityNotFoundException("Service provider with email " + email + " not found");
        }
        serviceProviderRepository.deleteByEmail(email);
    }

    public ServiceProvider findOrCreateProvider(String email, String name) {
        ServiceProvider existingProvider = serviceProviderRepository.findByEmail(email).orElse(null);
        if (existingProvider != null) {
            return existingProvider;
        }
        ServiceProvider newProvider = new ServiceProvider();
        newProvider.setEmail(email);
        newProvider.setName(name);
        newProvider.setRoles(Set.of(roleService.getRoleOrThrow("ROLE_PROVIDER")));

        return serviceProviderRepository.save(newProvider);
    }


    public void uploadPictureSet(MultipartFile[] workPictureList, MultipartFile profilePicture, ServiceProvider serviceProvider) {
        if (profilePicture != null) {
            Picture picture = new Picture();
            picture.setServiceProvider(serviceProvider);
            String url = uploadImage(profilePicture);
            picture.setUrl(url);
            serviceProvider.setProfilePicture(url);
            pictureRepository.save(picture);
        }
        if (workPictureList != null) {
            Arrays.stream(workPictureList).forEach(file -> {
                Picture picture = new Picture();
                picture.setServiceProvider(serviceProvider);
                picture.setUrl(uploadImage(file));
                serviceProvider.getWorkPictureSet().add(picture);
                pictureRepository.save(picture);
            });
        }
    }

    public String uploadImage(MultipartFile image) {
        if (image != null) {
            String imageName = MediaConstants.PROVIDER_FOLDER + UUID.randomUUID();
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
