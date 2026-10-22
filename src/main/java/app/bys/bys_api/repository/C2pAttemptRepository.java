package app.bys.bys_api.repository;

import app.bys.bys_api.model.entity.C2pAttempt;
import app.bys.bys_api.model.enums.C2pAttemptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface C2pAttemptRepository extends JpaRepository<C2pAttempt, Long> {

    long countByOfferIdAndStatus(Long offerId, C2pAttemptStatus status);

    boolean existsByOfferIdAndStatusIn(Long offerId, Collection<C2pAttemptStatus> statuses);

    List<C2pAttempt> findByStatusInAndCreatedAtBefore(Collection<C2pAttemptStatus> statuses, LocalDateTime before);

    boolean existsByBancamigaNrorefAndStatus(String bancamigaNroref, C2pAttemptStatus status);

    @Query("""
            SELECT a FROM C2pAttempt a
            WHERE (:status IS NULL OR a.status = :status)
            ORDER BY a.createdAt DESC
            """)
    Page<C2pAttempt> findAllByStatus(@Param("status") C2pAttemptStatus status, Pageable pageable);
}
