package app.bys.bys_api.service;

import app.bys.bys_api.mapper.FinalUserMapper;
import app.bys.bys_api.mapper.PageMapper;
import app.bys.bys_api.mapper.ServiceRequestMapper;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.dto.ServiceRequestDto;
import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.model.entity.ServiceRequest;
import app.bys.bys_api.repository.FinalUserRepository;
import app.bys.bys_api.repository.ServiceRequestRepository;
import app.bys.bys_api.service.specification.ServiceRequestSpecification;
import app.bys.bys_api.utils.specification.SearchCriteria;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceRequestService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final FinalUserRepository finalUserRepository;
    private final ServiceRequestMapper requestMapper;
    private final FinalUserMapper userMapper;

    public ServiceRequestDto get(Long id) {
        return requestMapper.entityToDto(serviceRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Service request with id: " + id + " not found")));
    }

    public PageDto<ServiceRequestDto> getAll(Pageable pageable, String search, List<Long> specializationList, String address, List<Long> userList, List<Long> providerList) {
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
                pageable).map(requestMapper::entityToDto));
    }

    public ServiceRequest createWithId(Long id, ServiceRequestDto serviceRequestDto) {
        FinalUser finalUser = finalUserRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Final user with id: " + id + " not found"));
        serviceRequestDto.setFinalUser(userMapper.entityToDto(finalUser));
        return serviceRequestRepository.save(requestMapper.dtoToEntity(serviceRequestDto));
    }


    public ServiceRequest create(String email, ServiceRequestDto serviceRequestDto) {
        FinalUser finalUser = finalUserRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Final user with email: " + email + " not found"));

        serviceRequestDto.setFinalUser(userMapper.entityToDto(finalUser));
        return serviceRequestRepository.save(requestMapper.dtoToEntity(serviceRequestDto));
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
}
