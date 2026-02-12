package app.bys.bys_api.controller;

import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.dto.ServiceCatalogDto;
import app.bys.bys_api.service.ServiceCatalogService;
import app.bys.bys_api.validation.OnCreate;
import app.bys.bys_api.validation.OnUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/service_catalog")
public class ServiceCatalogController {

    private final ServiceCatalogService serviceCatalogService;

    @GetMapping("/{id}")
    public ResponseEntity<ServiceCatalogDto> getServiceCatalog(@PathVariable Long id) {
        return ResponseEntity.ok(serviceCatalogService.get(id));
    }

    @GetMapping
    public ResponseEntity<PageDto<ServiceCatalogDto>> getAllServiceCatalog(@RequestParam(name = "search", required = false) String search,
                                                                           @RequestParam(name = "description", required = false) String description,
                                                                           @RequestParam(name = "specialization", required = false) List<Long> specializationList,
                                                                           Pageable pageable) {
        return ResponseEntity.ok(serviceCatalogService.getAll(search, description, specializationList, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ServiceCatalogDto> createServiceCatalog(@Validated(OnCreate.class) @RequestPart(name = "service_catalog") ServiceCatalogDto serviceCatalogDto,
                                                                  @RequestPart(name = "pictures", required = false) MultipartFile[] files) {
        return ResponseEntity.ok(serviceCatalogService.create(serviceCatalogDto, files));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ServiceCatalogDto> updateServiceCatalog(@PathVariable Long id,
                                                                  @Validated(OnUpdate.class) @RequestPart(name = "service_catalog") ServiceCatalogDto serviceCatalogDto,
                                                                  @RequestPart(name = "pictures", required = false) MultipartFile[] files) {
        return ResponseEntity.ok(serviceCatalogService.update(id, serviceCatalogDto, files));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> deleteServiceCatalog(@PathVariable Long id) {
        serviceCatalogService.delete(id);
        return ResponseEntity.ok().build();
    }
}
