package app.bys.bys_api.repository;

import app.bys.bys_api.model.entity.Specialization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface SpecializationRepository extends JpaRepository<Specialization, Long>, JpaSpecificationExecutor<Specialization> {
    boolean existsBySpecializationType(String specializationType);

    @Query("""
                SELECT s
                FROM ServiceProvider sp
                JOIN sp.specializations s
                WHERE sp.id = :serviceProviderId
            """)
    Set<Specialization> findByServiceProviderId(@Param("serviceProviderId") Long serviceProviderId);
}
