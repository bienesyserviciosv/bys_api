package app.bys.bys_api.repository;

import app.bys.bys_api.model.dto.FinalUserMetricsDto;
import app.bys.bys_api.model.entity.FinalUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface FinalUserRepository extends JpaRepository<FinalUser, Long>, JpaSpecificationExecutor<FinalUser> {

    @Query("SELECT COUNT(fu) FROM FinalUser fu")
    long countAllFinalUser();

    Optional<FinalUser> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    Optional<FinalUser> findByPhoneNumber(String phoneNumber);

    void deleteByEmail(String email);

    @Query("SELECT fu FROM FinalUser fu JOIN fu.roles r WHERE r.name = 'ROLE_ADMIN'")
    List<FinalUser> findAdmins();

    @Query("SELECT fu FROM FinalUser fu JOIN fu.roles r WHERE r.name = 'ROLE_SUPER_ADMIN'")
    FinalUser findSuperAdmin();

    Page<FinalUser> findByRoles_Name(String roleName, Pageable pageable);

    @Query("""
            SELECT new app.bys.bys_api.model.dto.FinalUserMetricsDto(
                u.id,
                u.name,
                u.email,
                u.phoneNumber,
                u.registrationDate,
                u.lastLoginDate,
                COUNT(r),
                SUM(CASE WHEN r.requestStatus = 'ACCEPTED' THEN 1 ELSE 0 END),
                SUM(CASE WHEN r.requestStatus = 'REJECTED' THEN 1 ELSE 0 END),
                SUM(CASE WHEN r.requestStatus = 'PENDING' THEN 1 ELSE 0 END)
            )
            FROM FinalUser u
            JOIN u.roles role
            LEFT JOIN u.serviceRequestSet r
            WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%'))
                AND role.name = 'ROLE_USER'
            GROUP BY u.id, u.name, u.email, u.phoneNumber, u.registrationDate, u.lastLoginDate
            """)
    Page<FinalUserMetricsDto> findAllUserMetrics(@Param("name") String name, Pageable pageable);

    @Query("""
            SELECT new app.bys.bys_api.model.dto.FinalUserMetricsDto(
                u.id,
                u.name,
                u.email,
                u.phoneNumber,
                u.registrationDate,
                u.lastLoginDate,
                COUNT(r),
                SUM(CASE WHEN r.requestStatus = 'ACCEPTED' THEN 1 ELSE 0 END),
                SUM(CASE WHEN r.requestStatus = 'REJECTED' THEN 1 ELSE 0 END),
                SUM(CASE WHEN r.requestStatus = 'PENDING' THEN 1 ELSE 0 END)
            )
            FROM FinalUser u
            JOIN u.roles role
            LEFT JOIN u.serviceRequestSet r
            WHERE u.id = :userId
                AND role.name = 'ROLE_USER'
            GROUP BY u.id, u.name, u.email, u.phoneNumber, u.registrationDate, u.lastLoginDate
            """)
    Optional<FinalUserMetricsDto> findUserMetricsById(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE FinalUser u SET u.fcmToken = :token WHERE u.id = :userId")
    void updateFcmToken(@Param("userId") Long userId, @Param("token") String token);
}
