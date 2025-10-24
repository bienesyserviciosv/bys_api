package app.bys.bys_api.repository;

import app.bys.bys_api.model.dto.ServiceProviderSummary;
import app.bys.bys_api.model.dto.ServiceProviderWithPictureFlatDto;
import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.model.enums.MembershipType;
import app.bys.bys_api.model.enums.Province;
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
public interface ServiceProviderRepository extends JpaRepository<ServiceProvider, Long>, JpaSpecificationExecutor<ServiceProvider> {
    Optional<ServiceProvider> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    Optional<ServiceProvider> findByPhoneNumber(String phone);

    List<ServiceProvider> findBySpecializations_Id(Long specializationId);

    void deleteByEmail(String email);

    @Query("""
    SELECT new app.bys.bys_api.model.dto.ServiceProviderWithPictureFlatDto(
        sp.id,
        sp.name,
        sp.email,
        sp.phoneNumber,
        sp.address,
        sp.experience,
        sp.verified,
        sp.membershipType,
        sp.registrationDate,
        sp.lastLoginDate,
        sp.completedServices,
        sp.qualification,
        sp.profilePicture,
        sp.status,
        r.name
    )
    FROM ServiceProvider sp
    JOIN sp.roles r
    WHERE sp.id = :id
      AND r.name = 'ROLE_PROVIDER'
    """)
    Optional<ServiceProviderWithPictureFlatDto> findFlatDtoById(@Param("id") Long id);

    @Query("""
            SELECT new app.bys.bys_api.model.dto.ServiceProviderSummary(
                sp.id,
                sp.name,
                sp.email,
                sp.phoneNumber,
                sp.address,
                sp.experience,
                sp.verified,
                sp.membershipType,
                sp.registrationDate,
                sp.lastLoginDate,
                sp.completedServices,
                sp.qualification,
                sp.status,
                r.name
            )
            FROM ServiceProvider sp
            JOIN sp.roles r
            WHERE r.name = 'ROLE_PROVIDER'
              AND (:search IS NULL OR LOWER(sp.name) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:address IS NULL OR sp.address = :address)
              AND (:membershipType IS NULL OR sp.membershipType = :membershipType)
              AND (:verified IS NULL OR sp.verified = :verified)
              AND (:specializationList IS NULL OR EXISTS (
                    SELECT s FROM sp.specializations s WHERE s.id IN :specializationList
              ))
            """)
    Page<ServiceProviderSummary> findAllProviderSummariesFiltered(
            @Param("search") String search,
            @Param("specializationList") List<Long> specializationList,
            @Param("address") Province address,
            @Param("membershipType") MembershipType membershipType,
            @Param("verified") Boolean verified,
            Pageable pageable
    );

}
