package app.bys.bys_api.repository;

import app.bys.bys_api.model.entity.FinalUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FinalUserRepository extends JpaRepository<FinalUser, Long>, JpaSpecificationExecutor<FinalUser> {

    Optional<FinalUser> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    Optional<FinalUser> findByPhoneNumber(String phoneNumber);

    void deleteByEmail(String email);

    @Query("SELECT fu FROM FinalUser fu JOIN fu.roles r WHERE r.name = 'ROLE_ADMIN'")
    List<FinalUser> findAdminsToNotify();

    Page<FinalUser> findByRoles_Name(String roleName, Pageable pageable);
}
