package app.bys.bys_api.mapper;

import app.bys.bys_api.model.dto.ServiceRequestDto;
import app.bys.bys_api.model.dto.ServiceRequestWithPictureDto;
import app.bys.bys_api.model.entity.Picture;
import app.bys.bys_api.model.entity.ServiceRequest;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public abstract class ServiceRequestMapper {

    @Value("${media.url}")
    public String mediaUrl;

    @AfterMapping
    @BeanMapping(builder = @Builder(disableBuilder = true))
    public void addMediaUrlToImage(@MappingTarget ServiceRequestWithPictureDto requestDto) {
        requestDto.setPictureSet(
                requestDto.getPictureSet().stream()
                        .filter(Objects::nonNull)
                        .map(url -> url.startsWith(mediaUrl) ? url : mediaUrl + url)
                        .collect(Collectors.toSet()));
    }

    @BeforeMapping
    @BeanMapping(builder = @Builder(disableBuilder = true))
    public void pictureToUrl(@MappingTarget ServiceRequestWithPictureDto requestDto, ServiceRequest request) {
        requestDto.setPictureSet(request.getPictureSet().stream()
                .map(Picture::getUrl)
                .collect(Collectors.toSet()));
    }

    public abstract ServiceRequest dtoToEntity(ServiceRequestDto serviceRequestDto);

    public abstract ServiceRequestDto entityToDto(ServiceRequest serviceRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract void updateServiceRequestFromDto(ServiceRequestDto serviceRequestDto, @MappingTarget ServiceRequest serviceRequest);

    @Mapping(target = "pictureSet", source = "pictureSet", qualifiedByName = "pictureToUrlSet")
    public abstract ServiceRequestWithPictureDto entityToDtoWithPicture(ServiceRequest serviceRequest);

    @Named("pictureToUrlSet")
    public Set<String> pictureToUrlSet(Set<Picture> pictures) {
        if (pictures == null) return Set.of();
        return pictures.stream()
                .map(Picture::getUrl)
                .collect(Collectors.toSet());
    }

//    @Named("pictureToUrl")
//    default String pictureToUrl(Picture picture) {
//        return picture.getUrl();
//    }
//
//    @IterableMapping(qualifiedByName = "pictureToUrl")
//    @Named("pictureSetToUrlSet")
//    default Set<String> pictureSetToUrlSet(Set<Picture> pictures) {
//        return pictures.stream()
//                .map(this::pictureToUrl)
//                .collect(Collectors.toSet());
//    }
}

