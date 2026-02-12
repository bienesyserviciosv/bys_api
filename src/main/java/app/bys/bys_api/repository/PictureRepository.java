package app.bys.bys_api.repository;

import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.model.entity.Picture;
import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.model.enums.PictureType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PictureRepository extends JpaRepository<Picture, Long> {

    @Query("""
            SELECT p.url
            FROM Picture p
            WHERE p.serviceRequest.id = :id
              AND p.pictureType = app.bys.bys_api.model.enums.PictureType.SERVICE_REQUEST
            """)
    Set<String> findPictureUrlByServiceRequestId(@Param("id") Long serviceRequestId);

    Optional<Picture> findByFinalUserId(Long finalUserId);

    @Modifying
    @Query("DELETE FROM Picture p WHERE p.finalUser = :user")
    void deleteByFinalUser(@Param("user") FinalUser user);

    Optional<Picture> findByPaymentId(Long id);

    void deleteByServiceProviderAndUrl(ServiceProvider serviceProvider, String url);

    List<Picture> findByServiceProviderAndPictureType(ServiceProvider serviceProvider, PictureType pictureType);

    @Query("SELECT p FROM Picture p WHERE p.serviceProvider.id = :providerId AND p.pictureType = 'PROFILE'")
    Optional<Picture> findProfilePictureByServiceProviderId(@Param("providerId") Long providerId);

    @Query("""
            SELECT p.url
            FROM Picture p
            WHERE p.serviceProvider.id = :id
              AND p.pictureType = app.bys.bys_api.model.enums.PictureType.WORK
            """)
    Set<String> findWorkPictureUrlsByProviderId(@Param("id") Long id);

    @Query("""
                SELECT p.url
                FROM Picture p
                WHERE p.serviceCatalog.id = :id
            """)
    List<String> findPicturesByCatalogId(Long id);

    @Query("""
                SELECT p.serviceCatalog.id, p.url
                FROM Picture p
                WHERE p.serviceCatalog IS NOT NULL
            """)
    List<Object[]> findAllPictures();


}
