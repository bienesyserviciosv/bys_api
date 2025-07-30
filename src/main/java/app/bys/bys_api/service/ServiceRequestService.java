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
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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

    public PageDto<ServiceRequestDto> getAll(Pageable pageable) {
        return PageMapper.pageToDto(serviceRequestRepository.findAll(pageable).map(requestMapper::entityToDto));
    }

    public ServiceRequestDto create(Long id, ServiceRequestDto serviceRequestDto) {
        FinalUser finalUser = finalUserRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Final user with id: " + id + " not found"));

        serviceRequestDto.setFinalUser(userMapper.entityToDto(finalUser));
        finalUserRepository.save(finalUser);
        return requestMapper.entityToDto(serviceRequestRepository.save(requestMapper.dtoToEntity(serviceRequestDto)));
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
