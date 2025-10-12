package app.bys.bys_api.repository;

import app.bys.bys_api.model.dto.OfferDto;
import app.bys.bys_api.model.dto.OfferMetricsDto;
import app.bys.bys_api.model.entity.Offer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long>, JpaSpecificationExecutor<Offer> {

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
                o.accepted,
                o.createdAt,
                o.acceptedAt
            )
            FROM Offer o
            JOIN o.serviceRequest sr
            JOIN o.provider sp
            LEFT JOIN o.finalUser fu
            WHERE (:search IS NULL OR LOWER(o.description) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:serviceRequestIds IS NULL OR sr.id IN :serviceRequestIds)
              AND (:providerIds IS NULL OR sp.id IN :providerIds)
              AND (:userIds IS NULL OR fu.id IN :userIds)
              AND (:accepted IS NULL OR o.accepted = :accepted)
            """)
    Page<OfferDto> findAllOffersFiltered(
            @Param("search") String search,
            @Param("serviceRequestIds") List<Long> serviceRequestIds,
            @Param("providerIds") List<Long> providerIds,
            @Param("accepted") Boolean accepted,
            @Param("userIds") List<Long> userIds,
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
                o.accepted,
                o.acceptedAt
            )
            FROM Offer o
            JOIN o.serviceRequest sr
            JOIN o.provider sp
            WHERE (:search IS NULL OR LOWER(o.description) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:serviceRequestIds IS NULL OR sr.id IN :serviceRequestIds)
              AND (:providerIds IS NULL OR sp.id IN :providerIds)
              AND (:accepted IS NULL OR o.accepted = :accepted)
            """)
    Page<OfferMetricsDto> findAllOfferMetricsFiltered(
            @Param("search") String search,
            @Param("serviceRequestIds") List<Long> serviceRequestIds,
            @Param("providerIds") List<Long> providerIds,
            @Param("accepted") Boolean accepted,
            Pageable pageable
    );
}