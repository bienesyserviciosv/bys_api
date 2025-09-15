package app.bys.bys_api.repository;

import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.model.entity.Picture;
import app.bys.bys_api.model.entity.ServiceProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PictureRepository extends JpaRepository<Picture, Long> {

    List<Picture> findByServiceRequestId(Long serviceRequestId);

    @Query("SELECT p FROM Picture p WHERE p.serviceProvider.id = :providerId AND p.pictureType = 'PROFILE'")
    Optional<Picture> findProfilePictureByServiceProviderId(@Param("providerId") Long providerId);

    Optional<Picture> findByFinalUserId(Long finalUserId);

    @Modifying
    @Query("DELETE FROM Picture p WHERE p.finalUser = :user")
    void deleteByFinalUser(@Param("user") FinalUser user);

    Optional<Picture> findByPaymentId(Long id);

    void deleteByServiceProviderAndUrl(ServiceProvider serviceProvider, String url);
}
