package app.bys.bys_api.repository;

import app.bys.bys_api.model.entity.ServiceProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServiceProviderRepository extends JpaRepository<ServiceProvider, Long> {
     Optional<ServiceProvider> findByEmail(String email);
}
