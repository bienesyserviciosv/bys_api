package app.bys.bys_api.mapper;

import app.bys.bys_api.model.dto.OfferDto;
import app.bys.bys_api.model.entity.Offer;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface OfferMapper {

    Offer dtoToEntity(OfferDto offerDto);

    @Mapping(target = "providerId", source = "provider.id")
    OfferDto entityToDto(Offer offer);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateOfferFromDto(OfferDto offerDto, @MappingTarget Offer offer);
}
