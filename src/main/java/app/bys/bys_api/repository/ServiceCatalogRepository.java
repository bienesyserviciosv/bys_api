package app.bys.bys_api.repository;

import app.bys.bys_api.model.dto.ServiceCatalogDto;
import app.bys.bys_api.model.entity.ServiceCatalog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServiceCatalogRepository extends JpaRepository<ServiceCatalog, Long>, JpaSpecificationExecutor<ServiceCatalog> {

    @Query("""
    SELECT new app.bys.bys_api.model.dto.ServiceCatalogDto(
        sc.id,
        sc.name,
        sc.description,
        sc.specialization.id,
        sc.specialization.specializationType
    )
    FROM ServiceCatalog sc
    WHERE sc.id = :id
""")
    Optional<ServiceCatalogDto> findCatalogById(Long id);

    @Query("""
                SELECT new app.bys.bys_api.model.dto.ServiceCatalogDto(
                    sc.id,
                    sc.name,
                    sc.description,
                    sc.specialization.id,
                    sc.specialization.specializationType
                )
                FROM ServiceCatalog sc
                WHERE (:search IS NULL OR LOWER(sc.name) LIKE LOWER(CONCAT('%', :search, '%')))
                      AND (:description IS NULL OR LOWER(sc.description) LIKE LOWER(CONCAT('%', :description, '%')))
                      AND (:specializationIdList IS NULL OR sc.specialization.id IN :specializationIdList)
            """)
    Page<ServiceCatalogDto> findAllCatalogs(
            @Param("search") String search,
            @Param("description") String description,
            @Param("specializationIdList") List<Long> specializationIdList,
            Pageable pageable
    );


}
