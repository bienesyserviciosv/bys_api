package app.bys.bys_api.mapper;

import app.bys.bys_api.model.dto.PictureDto;
import app.bys.bys_api.model.entity.Picture;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PictureMapper {

    Picture dtoToEntity(PictureDto pictureDto);

    PictureDto entityToDto(Picture picture);
}
