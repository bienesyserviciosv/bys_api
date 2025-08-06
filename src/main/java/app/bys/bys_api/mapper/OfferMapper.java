package app.bys.bys_api.mapper;

import app.bys.bys_api.model.dto.OfferDto;
import app.bys.bys_api.model.entity.Offer;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface OfferMapper {

    Offer dtoToEntity(OfferDto offerDto);

    OfferDto entityToDto(Offer offer);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateOfferFromDto(OfferDto offerDto, @MappingTarget Offer offer);
}
