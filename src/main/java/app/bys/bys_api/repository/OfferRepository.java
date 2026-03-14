package app.bys.bys_api.repository;

import app.bys.bys_api.model.dto.OfferDto;
import app.bys.bys_api.model.dto.OfferMetricsDto;
import app.bys.bys_api.model.entity.Offer;
import app.bys.bys_api.model.enums.OfferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long>, JpaSpecificationExecutor<Offer> {

    boolean existsByProviderIdAndServiceRequestId(Long providerId, Long requestId);

    @Query("SELECT COUNT(o) FROM Offer o")
    long countAllOffers();

    @Query("""
                SELECT COALESCE(SUM(o.price), 0)
                FROM Offer o
                WHERE o.serviceRequest.requestStatus = app.bys.bys_api.model.enums.RequestStatus.ACCEPTED
            """)
    Double calculateTotalProfit();


    @Query("""
            SELECT new app.bys.bys_api.model.dto.OfferDto(
                o.id,
                o.price,
                o.duration,
                o.description,
                sr.id,
                sp.id,
                o.status,
                o.createdAt,
                o.acceptedAt
            )
            FROM Offer o
            JOIN o.serviceRequest sr
            JOIN o.provider sp
            LEFT JOIN o.finalUser fu
            WHERE o.id = :id
            """)
    Optional<OfferDto> findOfferById(@Param("id") Long id);

    @Query("""
            SELECT new app.bys.bys_api.model.dto.OfferDto(
                o.id,
                o.price,
                o.duration,
                o.description,
                sr.id,
                sp.id,
                o.status,
                o.createdAt,
                o.acceptedAt
            )
            FROM Offer o
            JOIN o.serviceRequest sr
            JOIN o.provider sp
            LEFT JOIN o.finalUser fu
            WHERE (:search IS NULL OR LOWER(o.description) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:serviceRequestId IS NULL OR sr.id = :serviceRequestId)
              AND (:providerIds IS NULL OR sp.id IN :providerIds)
              AND (:userIds IS NULL OR fu.id IN :userIds)
              AND (:status IS NULL OR o.status = :status)
              AND (:excludeCompleted IS NULL OR :excludeCompleted = false OR o.status <> app.bys.bys_api.model.enums.OfferStatus.COMPLETED)
            """)
    Page<OfferDto> findAllOffersFiltered(
            @Param("search") String search,
            @Param("serviceRequestId") Long serviceRequestId,
            @Param("providerIds") List<Long> providerIds,
            @Param("status") OfferStatus status,
            @Param("userIds") List<Long> userIds,
            @Param("excludeCompleted") Boolean excludeCompleted,
            Pageable pageable
    );

    @Query("""
            SELECT new app.bys.bys_api.model.dto.OfferMetricsDto(
                o.id,
                sp.name,
                o.description,
                o.price,
                sr.id,
                o.createdAt,
                o.status,
                o.acceptedAt
            )
            FROM Offer o
            JOIN o.serviceRequest sr
            JOIN o.provider sp
            WHERE (:search IS NULL OR LOWER(o.description) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:serviceRequestIds IS NULL OR sr.id IN :serviceRequestIds)
              AND (:providerIds IS NULL OR sp.id IN :providerIds)
              AND (:status IS NULL OR o.status = :status)
              AND (:excludeCompleted IS NULL OR :excludeCompleted = false OR o.status <> app.bys.bys_api.model.enums.OfferStatus.COMPLETED)
            """)
    Page<OfferMetricsDto> findAllOfferMetricsFiltered(
            @Param("search") String search,
            @Param("serviceRequestIds") List<Long> serviceRequestIds,
            @Param("providerIds") List<Long> providerIds,
            @Param("status") OfferStatus status,
            @Param("excludeCompleted") Boolean excludeCompleted,
            Pageable pageable
    );
}