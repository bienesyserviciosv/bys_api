package app.bys.bys_api.repository;

import app.bys.bys_api.model.entity.Picture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PictureRepository extends JpaRepository<Picture, Long> {

    List<Picture> findByServiceRequestId(Long serviceRequestId);
    List<Picture> findByServiceProviderId(Long serviceProviderId);
    Picture findByFinalUserId(Long finalUserId);
}
