package app.bys.bys_api.mapper;

import app.bys.bys_api.model.dto.NotificationDto;
import app.bys.bys_api.model.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(source = "recipient.id", target = "recipient")
    @Mapping(source = "serviceRequest.id", target = "serviceRequestId")
    NotificationDto toDto(Notification notification);

    List<NotificationDto> toDtoList(List<Notification> notifications);
}

