package app.bys.bys_api.mapper;

import app.bys.bys_api.model.dto.ServiceRequestDto;
import app.bys.bys_api.model.entity.ServiceRequest;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ServiceRequestMapper {

    ServiceRequest dtoToEntity(ServiceRequestDto serviceRequestDto);

    ServiceRequestDto entityToDto(ServiceRequest serviceRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateServiceRequestFromDto(ServiceRequestDto serviceRequestDto, @MappingTarget ServiceRequest serviceRequest);
}

