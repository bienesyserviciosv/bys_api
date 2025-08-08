package app.bys.bys_api.repository;

import app.bys.bys_api.model.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {

    List<Notification> findByRecipientIdAndReadFalseOrderByTimestampDesc(Long providerId);

    List<Notification> findByRecipientIdOrderByTimestampDesc(Long providerId);
}
