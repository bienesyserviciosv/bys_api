package app.bys.bys_api.mapper;

import app.bys.bys_api.model.dto.ServiceProviderDto;
import app.bys.bys_api.model.dto.ServiceProviderWithPictureDto;
import app.bys.bys_api.model.dto.ServiceProviderWithPictureFlatDto;
import app.bys.bys_api.model.dto.SpecializationDto;
import app.bys.bys_api.model.entity.Picture;
import app.bys.bys_api.model.entity.Role;
import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.model.enums.PictureType;
import app.bys.bys_api.service.RoleService;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public abstract class ServiceProviderMapper {

    @Autowired
    private RoleService roleService;

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

    @Mapping(source = "role", target = "roles")
    public abstract ServiceProvider dtoToEntity(ServiceProviderDto serviceProviderDto);

    @Mapping(source = "roles", target = "role")
    public abstract ServiceProviderDto entityToDto(ServiceProvider serviceProvider);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract void updateServiceProviderFromDto(ServiceProviderDto serviceProviderDto, @MappingTarget ServiceProvider serviceProvider);

    @Mapping(source = "roles", target = "role")
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

    protected String mapFirstRole(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) return null;
        return roles.iterator().next().getName();
    }

    protected Set<Role> mapRoleName(String roleName) {
        if (roleName == null) return null;
        return Set.of(roleService.getRoleOrThrow(roleName));
    }

    public ServiceProviderWithPictureDto enrichDto(ServiceProviderWithPictureFlatDto flatDto, Set<SpecializationDto> specs, Set<String> pictures) {
        return ServiceProviderWithPictureDto.builder()
                .id(flatDto.getId())
                .name(flatDto.getName())
                .email(flatDto.getEmail())
                .phoneNumber(flatDto.getPhoneNumber())
                .address(flatDto.getAddress())
                .experience(flatDto.getExperience())
                .verified(flatDto.getVerified())
                .membershipType(flatDto.getMembershipType())
                .registrationDate(flatDto.getRegistrationDate())
                .lastLoginDate(flatDto.getLastLoginDate())
                .completedServices(flatDto.getCompletedServices())
                .qualification(flatDto.getQualification())
                .profilePicture(mediaUrl + flatDto.getProfilePicture())
                .role(flatDto.getRole())
                .specializations(specs)
                .workPictureSet(pictures)
                .build();
    }
}
