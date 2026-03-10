package app.bys.bys_api.repository;

import app.bys.bys_api.model.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {

    boolean existsByServiceProviderAndServiceRequestAndNotificationType(
            app.bys.bys_api.model.entity.ServiceProvider serviceProvider,
            app.bys.bys_api.model.entity.ServiceRequest serviceRequest,
            app.bys.bys_api.model.enums.NotificationType notificationType);

    boolean existsByFinalUserAndServiceRequestAndNotificationType(
            app.bys.bys_api.model.entity.FinalUser finalUser,
            app.bys.bys_api.model.entity.ServiceRequest serviceRequest,
            app.bys.bys_api.model.enums.NotificationType notificationType);

    boolean existsByFinalUserAndOfferAndNotificationType(
            app.bys.bys_api.model.entity.FinalUser finalUser,
            app.bys.bys_api.model.entity.Offer offer,
            app.bys.bys_api.model.enums.NotificationType notificationType);

}
