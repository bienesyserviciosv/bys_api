package app.bys.bys_api.mapper;

import app.bys.bys_api.model.dto.ServiceCatalogDto;
import app.bys.bys_api.model.entity.Picture;
import app.bys.bys_api.model.entity.ServiceCatalog;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public abstract class ServiceCatalogMapper {

    @Value("${media.url}")
    public String mediaUrl;

    @Mapping(target = "servicePictures", ignore = true)
    public abstract ServiceCatalog dtoToEntity(ServiceCatalogDto dto);

    @Mapping(target = "servicePictures", source = "servicePictures", qualifiedByName = "pictureToUrlSet")
    public abstract ServiceCatalogDto entityToDto(ServiceCatalog entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "servicePictures", ignore = true)
    public abstract void updateServiceCatalogFromDto(ServiceCatalogDto serviceCatalogDto, @MappingTarget ServiceCatalog serviceCatalog);


    @AfterMapping
    @BeanMapping(builder = @Builder(disableBuilder = true))
    public void addMediaUrlToImage(@MappingTarget ServiceCatalogDto serviceCatalogDto) {
        serviceCatalogDto.setServicePictures(
                serviceCatalogDto.getServicePictures().stream()
                        .filter(Objects::nonNull)
                        .map(url -> url.startsWith(mediaUrl) ? url : mediaUrl + url)
                        .collect(Collectors.toSet()));
    }

    @Named("pictureToUrlSet")
    public Set<String> pictureToUrlSet(Set<Picture> pictures) {
        if (pictures == null) return Set.of();
        return pictures.stream()
                .map(Picture::getUrl)
                .collect(Collectors.toSet());
    }
}
