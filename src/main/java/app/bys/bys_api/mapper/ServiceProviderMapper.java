package app.bys.bys_api.mapper;

import app.bys.bys_api.model.dto.ServiceProviderDto;
import app.bys.bys_api.model.dto.ServiceProviderWithPictureDto;
import app.bys.bys_api.model.entity.Picture;
import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.model.enums.PictureType;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public abstract class ServiceProviderMapper {

    @Value("${media.url}")
    public String mediaUrl;

    @AfterMapping
    @BeanMapping(builder = @Builder(disableBuilder = true))
    public void addMediaUrlToImage(@MappingTarget ServiceProviderWithPictureDto providerDto, ServiceProvider provider) {
        providerDto.setWorkPictureSet(
                providerDto.getWorkPictureSet().stream()
                        .filter(Objects::nonNull)
                        .map(url -> url.startsWith(mediaUrl) ? url : mediaUrl + url)
                        .collect(Collectors.toSet()));
        if (providerDto.getProfilePicture() != null) {
            providerDto.setProfilePicture(mediaUrl + provider.getProfilePicture());
        }
    }

    @BeforeMapping
    @BeanMapping(builder = @Builder(disableBuilder = true))
    public void pictureToUrl(@MappingTarget ServiceProviderWithPictureDto providerDto, ServiceProvider provider) {
        providerDto.setWorkPictureSet(provider.getWorkPictureSet().stream()
                .map(Picture::getUrl)
                .collect(Collectors.toSet()));
    }


    public abstract ServiceProvider dtoToEntity(ServiceProviderDto serviceProviderDto);

    public abstract ServiceProviderDto entityToDto(ServiceProvider serviceProvider);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract void updateServiceProviderFromDto(ServiceProviderDto serviceProviderDto, @MappingTarget ServiceProvider serviceProvider);

    @Mapping(target = "workPictureSet", source = "workPictureSet", qualifiedByName = "pictureToUrlSet")
    public abstract ServiceProviderWithPictureDto entityToDtoWithPicture(ServiceProvider serviceProvider);

    @Named("pictureToUrlSet")
    public Set<String> pictureToUrlSet(Set<Picture> pictures) {
        if (pictures == null) return Set.of();
        return pictures.stream()
                .filter(p -> p.getPictureType() == PictureType.WORK)
                .map(Picture::getUrl)
                .collect(Collectors.toSet());
    }

}
