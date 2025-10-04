package app.bys.bys_api.repository;

import app.bys.bys_api.model.entity.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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
}
