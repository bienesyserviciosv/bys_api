package app.bys.bys_api.mapper;

import app.bys.bys_api.model.dto.OfferDto;
import app.bys.bys_api.model.entity.Offer;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface OfferMapper {

    @Mapping(target = "provider.id", source = "providerId")
    @Mapping(target = "serviceRequest.id", source = "serviceRequestId")
    Offer dtoToEntity(OfferDto offerDto);

    @Mapping(target = "providerId", source = "provider.id")
    @Mapping(target = "serviceRequestId", source = "serviceRequest.id")
    OfferDto entityToDto(Offer offer);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateOfferFromDto(OfferDto offerDto, @MappingTarget Offer offer);
}
