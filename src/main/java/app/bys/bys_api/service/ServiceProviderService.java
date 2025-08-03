package app.bys.bys_api.service;

import app.bys.bys_api.error.DuplicateEmailException;
import app.bys.bys_api.mapper.PageMapper;
import app.bys.bys_api.mapper.ServiceProviderMapper;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.dto.ServiceProviderDto;
import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.repository.ServiceProviderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class ServiceProviderService {

    private final ServiceProviderRepository serviceProviderRepository;
    private final ServiceProviderMapper mapper;
    private final RoleService roleService;

    public ServiceProviderDto get(Long id) {
        return mapper.entityToDto(serviceProviderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Service provider with id " + id + " not found")));
    }

    public PageDto<ServiceProviderDto> getAll(Pageable pageable) {
        return PageMapper.pageToDto(serviceProviderRepository.findAll(pageable).map(mapper::entityToDto));
    }

    public ServiceProviderDto create(ServiceProviderDto serviceProviderDto) {
        if (serviceProviderRepository.existsByEmail(serviceProviderDto.getEmail())) {
            throw new DuplicateEmailException("The email is already registered");
        }
        return mapper.entityToDto(serviceProviderRepository.save(mapper.dtoToEntity(serviceProviderDto)));
    }

    public ServiceProviderDto update(Long id, ServiceProviderDto serviceProviderDto) {
        ServiceProvider serviceProviderFound = serviceProviderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Service provider with id " + id + " not found"));

        mapper.updateServiceProviderFromDto(serviceProviderDto, serviceProviderFound);
        return mapper.entityToDto(serviceProviderRepository.save(serviceProviderFound));
    }

    public void delete(Long id) {
        if (!serviceProviderRepository.existsById(id)) {
            throw new EntityNotFoundException("Service provider with id " + id + " not found");
        }
        serviceProviderRepository.deleteById(id);
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
}
