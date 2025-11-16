package app.bys.bys_api.mapper;

import app.bys.bys_api.model.dto.NotificationDto;
import app.bys.bys_api.model.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(source = "serviceProvider.id", target = "serviceProviderId")
    @Mapping(source = "serviceRequest.id", target = "serviceRequestId")
    @Mapping(source = "finalUser.id", target = "finalUserId")
    @Mapping(source = "payment.id", target = "paymentId")
    @Mapping(source = "notificationType.message", target = "message")
    @Mapping(source = "offer.id", target = "offerId")
    NotificationDto toDto(Notification notification);

    List<NotificationDto> toDtoList(List<Notification> notifications);
}

