package app.bys.bys_api.mapper;

import app.bys.bys_api.model.dto.FinalUserDto;
import app.bys.bys_api.model.entity.FinalUser;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Value;

@Mapper(componentModel = "spring")
public abstract class FinalUserMapper {

    @Value("${media.url}")
    public String mediaUrl;

    @AfterMapping
    public void addMediaUrlToImage(@MappingTarget FinalUserDto userDto) {
        if (userDto.getProfilePicture() != null) {
            userDto.setProfilePicture(mediaUrl + userDto.getProfilePicture());
        }
    }

    public abstract FinalUser dtoToEntity(FinalUserDto finalUserDto);

    public abstract FinalUserDto entityToDto(FinalUser finalUser);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract void updateFinalUserFromDto(FinalUserDto finalUserDto, @MappingTarget FinalUser finalUser);
}