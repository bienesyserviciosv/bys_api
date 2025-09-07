package app.bys.bys_api.mapper;

import app.bys.bys_api.model.dto.SpecializationDto;
import app.bys.bys_api.model.entity.Specialization;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.Set;

@Mapper(componentModel = "spring")
public interface SpecializationMapper {

    Specialization dtoToEntity(SpecializationDto specializationDto);

    SpecializationDto entityToDto(Specialization specialization);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateSpecializationFromDto(SpecializationDto specializationDto, @MappingTarget Specialization specialization);

    Set<Specialization> setDtoToEntitySet(Set<SpecializationDto> specializationDtos);

    Set<SpecializationDto> setEntityToDtoSet(Set<Specialization> specializations);
}
