package app.bys.bys_api.service;

import app.bys.bys_api.mapper.PageMapper;
import app.bys.bys_api.mapper.ServiceRequestMapper;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.dto.ServiceRequestDto;
import app.bys.bys_api.model.entity.ServiceRequest;
import app.bys.bys_api.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ServiceRequestService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final ServiceRequestMapper mapper;

    public ServiceRequestDto get(Long id) {
        return mapper.entityToDto(serviceRequestRepository.findById(id).orElseThrow());
    }

    public PageDto<ServiceRequestDto> getAll(Pageable pageable) {
        return PageMapper.pageToDto(serviceRequestRepository.findAll(pageable).map(mapper::entityToDto));
    }

    public ServiceRequestDto create(ServiceRequestDto serviceRequestDto) {
        return mapper.entityToDto(serviceRequestRepository.save(mapper.dtoToEntity(serviceRequestDto)));
    }

    public ServiceRequestDto update(Long id, ServiceRequestDto serviceRequestDto) {
        ServiceRequest requestFound = serviceRequestRepository.findById(id).orElseThrow();
        mapper.updateServiceRequestFromDto(serviceRequestDto, requestFound);
        return mapper.entityToDto(serviceRequestRepository.save(requestFound));
    }

    public void delete(Long id) {
        serviceRequestRepository.deleteById(id);
    }
}
