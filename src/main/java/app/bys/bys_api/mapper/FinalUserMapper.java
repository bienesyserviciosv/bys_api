package app.bys.bys_api.mapper;

import app.bys.bys_api.model.dto.FinalUserDto;
import app.bys.bys_api.model.dto.FinalUserMetricsDto;
import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.model.entity.Role;
import app.bys.bys_api.service.RoleService;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Set;

@Mapper(componentModel = "spring")
public abstract class FinalUserMapper {

    @Autowired
    private RoleService roleService;

    @Value("${media.url}")
    public String mediaUrl;

    @AfterMapping
    public void addMediaUrlToImage(@MappingTarget FinalUserDto userDto) {
        if (userDto.getProfilePicture() != null) {
            userDto.setProfilePicture(mediaUrl + userDto.getProfilePicture());
        }
    }

    @Mapping(source = "role", target = "roles")
    public abstract FinalUser dtoToEntity(FinalUserDto finalUserDto);

    @Mapping(source = "roles", target = "role")
    public abstract FinalUserDto entityToDto(FinalUser finalUser);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract void updateFinalUserFromDto(FinalUserDto finalUserDto, @MappingTarget FinalUser finalUser);

    public abstract FinalUserMetricsDto entityToUserMetricsDto(FinalUser finalUser);

    protected String mapFirstRole(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) return null;
        return roles.iterator().next().getName();
    }

    protected Set<Role> mapRoleName(String roleName) {
        if (roleName == null) return null;
        return Set.of(roleService.getRoleOrThrow(roleName));
    }

}