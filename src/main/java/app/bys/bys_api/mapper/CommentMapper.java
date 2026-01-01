package app.bys.bys_api.mapper;

import app.bys.bys_api.model.dto.CommentDto;
import app.bys.bys_api.model.dto.UpdateCommentDto;
import app.bys.bys_api.model.entity.Comment;
import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.model.entity.ServiceRequest;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public abstract class CommentMapper {

//    @Mapping(source = "author", target = "author", qualifiedByName = "mapUser")
//    @Mapping(source = "provider", target = "provider", qualifiedByName = "mapProvider")
//    @Mapping(source = "request", target = "request", qualifiedByName = "mapRequest")
//    public abstract Comment dtoToEntity(CommentDto commentDto);

    @Mapping(source = "author.id", target = "author")
    @Mapping(source = "provider.id", target = "provider")
    @Mapping(source = "request.id", target = "request")
    public abstract CommentDto entityToDto(Comment comment);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract void updateCommentFromDto(UpdateCommentDto updateCommentDto, @MappingTarget Comment comment);

    @Named("mapUser")
    FinalUser mapUser(Long id) {
        if (id == null) return null;
        FinalUser u = new FinalUser();
        u.setId(id);
        return u;
    }
    @Named("mapProvider")
    ServiceProvider mapProvider(Long id) {
        if (id == null) return null;
        ServiceProvider p = new ServiceProvider();
        p.setId(id);
        return p;
    }

    @Named("mapRequest")
    ServiceRequest mapRequest(Long id) {
        if (id == null) return null;
        ServiceRequest r = new ServiceRequest();
        r.setId(id);
        return r;
    }
}
