package app.bys.bys_api.repository;

import app.bys.bys_api.model.dto.ServiceRequestMetricsDto;
import app.bys.bys_api.model.dto.ServiceRequestMinimal;
import app.bys.bys_api.model.dto.ServiceRequestSummary;
import app.bys.bys_api.model.entity.ServiceRequest;
import app.bys.bys_api.model.enums.Province;
import app.bys.bys_api.model.enums.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
            SELECT DISTINCT sr FROM ServiceRequest sr
            JOIN FETCH sr.finalUser fu
            JOIN FETCH sr.specialization s
            LEFT JOIN FETCH sr.comment c
            LEFT JOIN FETCH c.author
            LEFT JOIN FETCH c.provider
            LEFT JOIN FETCH sr.acceptedOffer ao
            LEFT JOIN FETCH ao.payment
            WHERE sr.id = :id
            """)
    Optional<ServiceRequest> findAdminInfoById(@Param("id") Long id);

    @Query(value = """
            SELECT DISTINCT sr FROM ServiceRequest sr
            JOIN FETCH sr.finalUser fu
            JOIN FETCH sr.specialization s
            LEFT JOIN FETCH sr.comment c
            LEFT JOIN FETCH c.author
            LEFT JOIN FETCH c.provider
            LEFT JOIN FETCH sr.acceptedOffer ao
            LEFT JOIN FETCH ao.payment
            LEFT JOIN FETCH sr.offerSet
            WHERE (:address IS NULL OR sr.address = :address)
              AND (:specializationList IS NULL OR s.id IN :specializationList)
              AND (:userList IS NULL OR fu.id IN :userList)
              AND sr.requestStatus IN :statusList
              AND sr.creationDate >= :createdFrom
              AND sr.creationDate < :createdToExclusive
            """,
            countQuery = """
            SELECT COUNT(DISTINCT sr.id) FROM ServiceRequest sr
            JOIN sr.finalUser fu
            JOIN sr.specialization s
            WHERE (:address IS NULL OR sr.address = :address)
              AND (:specializationList IS NULL OR s.id IN :specializationList)
              AND (:userList IS NULL OR fu.id IN :userList)
              AND sr.requestStatus IN :statusList
              AND sr.creationDate >= :createdFrom
              AND sr.creationDate < :createdToExclusive
            """)
    Page<ServiceRequest> findAllRequestInfo(
                                            @Param("specializationList") List<Long> specializationList,
                                            @Param("address") Province address,
                                            @Param("userList") List<Long> userList,
                                            @Param("statusList") List<RequestStatus> statusList,
                                            @Param("createdFrom") LocalDateTime createdFrom,
                                            @Param("createdToExclusive") LocalDateTime createdToExclusive,
                                            Pageable pageable
    );

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
              AND (:status IS NULL OR sr.requestStatus = :status)
              AND (
                     :status IS NOT NULL
                     OR sr.requestStatus <> app.bys.bys_api.model.enums.RequestStatus.COMPLETED
                  )
              AND (:allowedStatusForProviders IS NULL OR sr.requestStatus IN :allowedStatusForProviders)
              AND (:applyDateFilter = false OR sr.date >= :today)
            ORDER BY
                  CASE WHEN sr.creationDate >= :oneDayAgo THEN 0 ELSE 1 END ASC,
                  CASE WHEN sr.date <= :sevenDaysLater THEN 0 ELSE 1 END ASC,
                  CASE
                      WHEN sr.date <= :sevenDaysLater THEN sr.date
                      ELSE sr.creationDate
                  END ASC
            """)
    Page<ServiceRequestSummary> findAllRequestSummariesFiltered(
            @Param("search") String search,
            @Param("specializationList") List<Long> specializationList,
            @Param("address") Province address,
            @Param("userList") List<Long> userList,
            @Param("providerList") List<Long> providerList,
            @Param("status") RequestStatus status,
            @Param("allowedStatusForProviders") List<RequestStatus> allowedStatusForProviders,
            @Param("applyDateFilter") boolean applyDateFilter,
            @Param("today") LocalDate today,
            @Param("sevenDaysLater") LocalDate sevenDaysLater,
            @Param("oneDayAgo") LocalDateTime oneDayAgo,
            Pageable pageable
    );


    @Query("""
    SELECT new app.bys.bys_api.model.dto.ServiceRequestMinimal(
        sr.id,
        sr.requestStatus,
        sr.offerQuantity,
        sr.finalUser.id
    )
    FROM ServiceRequest sr
    WHERE sr.id = :id
""")
    Optional<ServiceRequestMinimal> findRequestMinimalById(Long id);

    @Modifying
    @Query("""
    UPDATE ServiceRequest sr
    SET sr.offerQuantity = sr.offerQuantity + 1,
        sr.newOffer = true
    WHERE sr.id = :id
""")
    void incrementOfferQuantity(Long id);


    @Query("""
    SELECT COUNT(sr) > 0
    FROM ServiceRequest sr
    WHERE sr.finalUser.id = :userId
      AND sr.specialization.id = :specializationId
      AND sr.requestStatus IN (
            app.bys.bys_api.model.enums.RequestStatus.CREATED,
            app.bys.bys_api.model.enums.RequestStatus.IN_PROGRESS,
            app.bys.bys_api.model.enums.RequestStatus.PENDING,
            app.bys.bys_api.model.enums.RequestStatus.ACCEPTED,
            app.bys.bys_api.model.enums.RequestStatus.IN_REVIEW
      )
""")
    boolean existsActiveRequestOfType(
            @Param("userId") Long userId,
            @Param("specializationId") Long specializationId
    );

}
