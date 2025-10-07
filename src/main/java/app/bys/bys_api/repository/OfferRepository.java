package app.bys.bys_api.repository;

import app.bys.bys_api.model.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long>, JpaSpecificationExecutor<Offer> {

    @Query("SELECT COUNT(o) FROM Offer o")
    long countAllOffers();

    @Query("""
    SELECT COALESCE(SUM(o.price), 0)
    FROM Offer o
    WHERE o.serviceRequest.requestStatus = app.bys.bys_api.model.enums.RequestStatus.ACCEPTED
""")
    Double calculateTotalProfit();

}
