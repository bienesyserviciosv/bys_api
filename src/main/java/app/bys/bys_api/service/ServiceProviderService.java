package app.bys.bys_api.service;

import app.bys.bys_api.error.DuplicateEmailException;
import app.bys.bys_api.error.DuplicatePhoneException;
import app.bys.bys_api.mapper.PageMapper;
import app.bys.bys_api.mapper.ServiceProviderMapper;
import app.bys.bys_api.mapper.SpecializationMapper;
import app.bys.bys_api.model.dto.*;
import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.model.entity.Specialization;
import app.bys.bys_api.model.enums.MembershipType;
import app.bys.bys_api.model.enums.Province;
import app.bys.bys_api.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceProviderService {

    private final ServiceProviderRepository serviceProviderRepository;
    private final ServiceProviderMapper mapper;
    private final RoleService roleService;
    private final SpecializationMapper specializationMapper;
    private final PictureService pictureService;
    private final SpecializationRepository specializationRepository;
    private final PictureRepository pictureRepository;
    @Value("${media.url}")
    public String mediaUrl;

    public ServiceProviderWithPictureDto get(Long id) {
        return getServiceProviderWithPictureDto(id);
    }

    private ServiceProviderWithPictureDto getServiceProviderWithPictureDto(Long id) {
        ServiceProviderWithPictureFlatDto providerFlatDto = serviceProviderRepository.findFlatDtoById(id)
                .orElseThrow(() -> new EntityNotFoundException("Service provider with id " + id + " not found"));
        Set<SpecializationDto> specializationDto = specializationMapper.setEntityToDtoSet(specializationRepository.findByServiceProviderId(id));
        Set<String> rawPictures = pictureRepository.findWorkPictureUrlsByProviderId(id);
        Set<String> workPictures = rawPictures.stream()
                .filter(Objects::nonNull)
                .map(url -> url.startsWith(mediaUrl) ? url : mediaUrl + url)
                .collect(Collectors.toSet());
        return mapper.enrichDto(providerFlatDto, specializationDto, workPictures);
    }

    public ServiceProviderWithPictureDto getWithEmail(String email) {
        ServiceProvider provider = serviceProviderRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Service provider with email " + email + " not found"));
        Long id = provider.getId();
        return getServiceProviderWithPictureDto(id);
    }

    public PageDto<ServiceProviderSummary> getAll(Pageable pageable, String search, List<Long> specializationList, String address, MembershipType membershipType, Boolean verified) throws BadRequestException {
        Province province = null;
        if (address != null) {
            try {
                province = Province.valueOf(address);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid province: " + address);
            }
        }
        if (search == null) {
            search = "";
        }
        if (specializationList != null && specializationList.isEmpty()) specializationList = null;
        Page<ServiceProviderSummary> page = serviceProviderRepository.findAllProviderSummariesFiltered(search, specializationList, province, membershipType, verified, pageable);

        page.forEach(dto -> {
            Set<String> specializationSet = specializationRepository.findByServiceProviderId(dto.getId())
                    .stream()
                    .map(Specialization::getSpecializationType)
                    .collect(Collectors.toSet());

            dto.setSpecializations(specializationSet);
        });

        return PageMapper.pageToDto(page);
    }

    public ServiceProviderWithPictureDto create(ServiceProviderDto serviceProviderDto, MultipartFile profilePicture, MultipartFile[] workPictureSet) {
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
                .completedServices(0)
                .address(serviceProviderDto.getAddress())
                .roles(Set.of(roleService.getRoleOrThrow("ROLE_PROVIDER")))
                .registrationDate(LocalDateTime.now())
                .build();

        ServiceProvider providerSaved = serviceProviderRepository.save(provider);
        pictureService.uploadProfilePictureForProvider(profilePicture, providerSaved);
        pictureService.uploadWorkPictures(workPictureSet, providerSaved);
        return mapper.entityToDtoWithPicture(providerSaved);
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

    public void updateLastLoginDate(ServiceProvider provider) {
        provider.setLastLoginDate(LocalDateTime.now());
        serviceProviderRepository.save(provider);
    }

}