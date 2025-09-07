package app.bys.bys_api.mapper;

import app.bys.bys_api.model.dto.ServiceProviderDto;
import app.bys.bys_api.model.dto.ServiceProviderWithPictureDto;
import app.bys.bys_api.model.entity.Picture;
import app.bys.bys_api.model.entity.ServiceProvider;
import org.mapstruct.*;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ServiceProviderMapper {

    ServiceProvider dtoToEntity(ServiceProviderDto serviceProviderDto);

    ServiceProviderDto entityToDto(ServiceProvider serviceProvider);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateServiceProviderFromDto(ServiceProviderDto serviceProviderDto, @MappingTarget ServiceProvider serviceProvider);

    @Mapping(target = "workPictureSet", source = "workPictureSet", qualifiedByName = "pictureSetToUrlSet")
    ServiceProviderWithPictureDto entityToDtoWithPicture(ServiceProvider serviceProvider);

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
