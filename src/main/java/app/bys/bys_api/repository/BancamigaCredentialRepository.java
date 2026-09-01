package app.bys.bys_api.repository;

import app.bys.bys_api.model.entity.BancamigaCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BancamigaCredentialRepository extends JpaRepository<BancamigaCredential, Long> {

    default BancamigaCredential findSingletonOrThrow() {
        return findById(1L)
                .orElseThrow(() -> new IllegalStateException(
                        "No hay credenciales de Bancamiga configuradas. Use POST /super_admin/bancamiga/credentials para sembrarlas."));
    }
}
