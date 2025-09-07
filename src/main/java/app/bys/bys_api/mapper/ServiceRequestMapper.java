package app.bys.bys_api.mapper;

import app.bys.bys_api.model.dto.ServiceRequestDto;
import app.bys.bys_api.model.dto.ServiceRequestWithPictureDto;
import app.bys.bys_api.model.entity.Picture;
import app.bys.bys_api.model.entity.ServiceRequest;
import org.mapstruct.*;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ServiceRequestMapper {

    ServiceRequest dtoToEntity(ServiceRequestDto serviceRequestDto);

    ServiceRequestDto entityToDto(ServiceRequest serviceRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateServiceRequestFromDto(ServiceRequestDto serviceRequestDto, @MappingTarget ServiceRequest serviceRequest);

    @Mapping(target = "pictureSet", source = "pictureSet", qualifiedByName = "pictureSetToUrlSet")
    ServiceRequestWithPictureDto entityToDtoWithPicture(ServiceRequest serviceRequest);

    @Named("pictureToUrl")
    default String pictureToUrl(Picture picture) {
        return picture.getUrl();
    }

    @IterableMapping(qualifiedByName = "pictureToUrl")
    @Named("pictureSetToUrlSet")
    default Set<String> pictureSetToUrlSet(Set<Picture> pictures) {
        return pictures.stream()
                .map(this::pictureToUrl)
                .collect(Collectors.toSet());
    }
}

