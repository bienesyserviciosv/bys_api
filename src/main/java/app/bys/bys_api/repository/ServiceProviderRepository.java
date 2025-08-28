package app.bys.bys_api.repository;

import app.bys.bys_api.model.entity.ServiceProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceProviderRepository extends JpaRepository<ServiceProvider, Long>, JpaSpecificationExecutor<ServiceProvider> {
     Optional<ServiceProvider> findByEmail(String email);

     boolean existsByEmail(String email);

     boolean existsByPhoneNumber(String phoneNumber);

     Optional<ServiceProvider> findByPhoneNumber(String phone);

     List<ServiceProvider> findByAddressAndSpecializations_Id(String address, Long specializationId);

     void deleteByEmail(String email);
}
