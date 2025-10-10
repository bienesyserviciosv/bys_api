package app.bys.bys_api.repository;

import app.bys.bys_api.model.dto.ServiceRequestMetricsDto;
import app.bys.bys_api.model.entity.ServiceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long>, JpaSpecificationExecutor<ServiceRequest> {

    @Query("SELECT COUNT(sr) FROM ServiceRequest sr")
    long countAllRequests();

    @Query(value = """
        SELECT AVG(EXTRACT(EPOCH FROM acceptance_date - creation_date)) / 3600
        FROM service_request
        WHERE creation_date IS NOT NULL AND acceptance_date IS NOT NULL
        """, nativeQuery = true)
    Double findAverageAcceptanceDurationInHours();

    @Query("""
            SELECT new app.bys.bys_api.model.dto.ServiceRequestMetricsDto(
                sr.id,
                fu.name,
                fu.email,
                fu.phoneNumber,
                sr.description,
                sr.date,
                sr.time,
                sr.requestStatus,
                sr.creationDate,
                sr.acceptanceDate,
                sr.offerQuantity,
                CASE WHEN p IS NOT NULL THEN ao.price ELSE NULL END,
                p.paymentType,
                p.paymentDate
            )
            FROM ServiceRequest sr
            JOIN sr.finalUser fu
            LEFT JOIN sr.acceptedOffer ao
            LEFT JOIN ao.payment p
            """)
    Page<ServiceRequestMetricsDto> findAllRequestMetrics(Pageable pageable);

    @Query("""
            SELECT new app.bys.bys_api.model.dto.ServiceRequestMetricsDto(
                sr.id,
                fu.name,
                fu.email,
                fu.phoneNumber,
                sr.description,
                sr.date,
                sr.time,
                sr.requestStatus,
                sr.creationDate,
                sr.acceptanceDate,
                sr.offerQuantity,
                CASE WHEN p IS NOT NULL THEN ao.price ELSE NULL END,
                p.paymentType,
                p.paymentDate
            )
            FROM ServiceRequest sr
            JOIN sr.finalUser fu
            LEFT JOIN sr.acceptedOffer ao
            LEFT JOIN ao.payment p
            WHERE sr.id = :requestId
            """)
    Optional<ServiceRequestMetricsDto> findRequestMetricsById(@Param("requestId") Long requestId);
}
