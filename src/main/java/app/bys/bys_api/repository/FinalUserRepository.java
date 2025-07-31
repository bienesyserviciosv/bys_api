package app.bys.bys_api.repository;

import app.bys.bys_api.model.entity.FinalUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FinalUserRepository extends JpaRepository<FinalUser, Long>, JpaSpecificationExecutor<FinalUser> {

    Optional<FinalUser> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);
}
