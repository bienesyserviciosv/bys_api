package app.bys.bys_api.repository;

import app.bys.bys_api.model.dto.ServiceRequestMetricsDto;
import app.bys.bys_api.model.dto.ServiceRequestSummary;
import app.bys.bys_api.model.entity.ServiceRequest;
import app.bys.bys_api.model.enums.Province;
import app.bys.bys_api.model.enums.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
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
                p.paymentDate,
                p.id
            )
            FROM ServiceRequest sr
            JOIN sr.finalUser fu
            LEFT JOIN sr.acceptedOffer ao
            LEFT JOIN ao.payment p
            JOIN sr.specialization s
            WHERE (:search IS NULL OR LOWER(sr.description) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:address IS NULL OR sr.address = :address)
              AND (:specializationList IS NULL OR s.id IN :specializationList)
              AND (:userList IS NULL OR fu.id IN :userList)
            """)
    Page<ServiceRequestMetricsDto> findAllRequestMetrics(
            @Param("search") String search,
            @Param("specializationList") List<Long> specializationList,
            @Param("address") Province address,
            @Param("userList") List<Long> userList,
            Pageable pageable
    );

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
                p.paymentDate,
                p.id
            )
            FROM ServiceRequest sr
            JOIN sr.finalUser fu
            LEFT JOIN sr.acceptedOffer ao
            LEFT JOIN ao.payment p
            WHERE sr.id = :requestId
            """)
    Optional<ServiceRequestMetricsDto> findRequestMetricsById(@Param("requestId") Long requestId);


    @Query("""
            SELECT new app.bys.bys_api.model.dto.ServiceRequestSummary(
                sr.id,
                sr.description,
                sr.date,
                sr.time,
                sr.latitude,
                sr.longitude,
                sr.requestStatus,
                sr.creationDate,
                sr.acceptanceDate,
                s.specializationType,
                fu.id,
                sr.offerQuantity,
                sr.newOffer,
                sr.serviceProvider.id
            )
            FROM ServiceRequest sr
            JOIN sr.specialization s
            JOIN sr.finalUser fu
            LEFT JOIN sr.serviceProvider sp
            WHERE sr.id = :id
            """)
    Optional<ServiceRequestSummary> findRequestById(@Param("id") Long id);

    @Query("""
            SELECT new app.bys.bys_api.model.dto.ServiceRequestSummary(
                sr.id,
                sr.description,
                sr.date,
                sr.time,
                sr.latitude,
                sr.longitude,
                sr.requestStatus,
                sr.creationDate,
                sr.acceptanceDate,
                s.specializationType,
                fu.id,
                sr.offerQuantity,
                sr.newOffer,
                sr.serviceProvider.id
            )
            FROM ServiceRequest sr
            JOIN sr.specialization s
            JOIN sr.finalUser fu
            LEFT JOIN sr.serviceProvider sp
            WHERE LOWER(sr.description) LIKE LOWER(CONCAT('%', :search, '%'))
              AND (:address IS NULL OR sr.address = :address)
              AND (:specializationList IS NULL OR s.id IN :specializationList)
              AND (:userList IS NULL OR fu.id IN :userList)
              AND (:providerList IS NULL OR sp.id IN :providerList)
              AND (:requestStatusList IS NULL OR sr.requestStatus IN :requestStatusList)
              AND (:applyDateFilter = false OR sr.date >= :today)
            """)
    Page<ServiceRequestSummary> findAllRequestSummariesFiltered(
            @Param("search") String search,
            @Param("specializationList") List<Long> specializationList,
            @Param("address") Province address,
            @Param("userList") List<Long> userList,
            @Param("providerList") List<Long> providerList,
            @Param("requestStatusList") List<RequestStatus> requestStatusList,
            @Param("applyDateFilter") boolean applyDateFilter,
            @Param("today") LocalDate today,
            Pageable pageable
    );

}
