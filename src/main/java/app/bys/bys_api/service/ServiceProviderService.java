package app.bys.bys_api.service;

import app.bys.bys_api.mapper.PageMapper;
import app.bys.bys_api.mapper.ServiceProviderMapper;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.dto.ServiceProviderDto;
import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.repository.ServiceProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ServiceProviderService {

    private final ServiceProviderRepository serviceProviderRepository;
    private final ServiceProviderMapper mapper;

    public ServiceProviderDto get(Long id) {
        return mapper.entityToDto(serviceProviderRepository.findById(id).orElseThrow());
    }

    public PageDto<ServiceProviderDto> getAll(Pageable pageable) {
        return PageMapper.pageToDto(serviceProviderRepository.findAll(pageable).map(mapper::entityToDto));
    }

    public ServiceProviderDto create(ServiceProviderDto serviceProviderDto) {
        return mapper.entityToDto(serviceProviderRepository.save(mapper.dtoToEntity(serviceProviderDto)));
    }

    public ServiceProviderDto update(Long id, ServiceProviderDto serviceProviderDto) {
        ServiceProvider serviceProviderFound = serviceProviderRepository.findById(id).orElseThrow();
        mapper.updateServiceProviderFromDto(serviceProviderDto, serviceProviderFound);
        return mapper.entityToDto(serviceProviderRepository.save(serviceProviderFound));
    }

    public void delete(Long id) {
        serviceProviderRepository.deleteById(id);
    }
}
