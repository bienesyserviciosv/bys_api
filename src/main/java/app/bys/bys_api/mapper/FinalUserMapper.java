package app.bys.bys_api.mapper;

import app.bys.bys_api.model.dto.FinalUserDto;
import app.bys.bys_api.model.entity.FinalUser;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface FinalUserMapper {

    FinalUser dtoToEntity(FinalUserDto finalUserDto);

    FinalUserDto entityToDto(FinalUser finalUser);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFinalUserFromDto(FinalUserDto finalUserDto, @MappingTarget FinalUser finalUser);
}

/*

@Mapper(componentModel = "spring", uses = {FinalUserMapper.class},builder = @Builder(disableBuilder = true))
public abstract class FinalUserMapper {

    public abstract FinalUser dtoToEntity(FinalUserDto finalUserDto);

    public abstract FinalUserDto entityToDto(FinalUser finalUser);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract void updateFinalUserFromDto(FinalUserDto finalUserDto, @MappingTarget FinalUser finalUser);

    public abstract Page<FinalUserDto> entityListToDtoList(Page<FinalUser> finalUserList);

    public abstract List<FinalUser> dtoListToEntityList(List<FinalUserDto> finalUserDtoList);

}

*/