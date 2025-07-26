package app.bys.bys_api.mapper;

import app.bys.bys_api.model.dto.ServiceProviderDto;
import app.bys.bys_api.model.entity.ServiceProvider;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ServiceProviderMapper {

    ServiceProvider dtoToEntity(ServiceProviderDto serviceProviderDto);

    ServiceProviderDto entityToDto(ServiceProvider serviceProvider);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateServiceProviderFromDto(ServiceProviderDto serviceProviderDto, @MappingTarget ServiceProvider serviceProvider);

}
