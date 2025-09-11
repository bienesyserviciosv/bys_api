package app.bys.bys_api.repository;

import app.bys.bys_api.model.entity.Picture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PictureRepository extends JpaRepository<Picture, Long> {

    List<Picture> findByServiceRequestId(Long serviceRequestId);
    List<Picture> findByServiceProviderId(Long serviceProviderId);
    Optional<Picture> findByFinalUserId(Long finalUserId);

    Optional<Picture> findByPaymentId(Long id);
}
