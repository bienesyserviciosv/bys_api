package app.bys.bys_api.controller.admin;

import app.bys.bys_api.model.dto.OfferDto;
import app.bys.bys_api.model.dto.OfferMetricsDto;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.repository.ServiceProviderRepository;
import app.bys.bys_api.service.OfferService;
import app.bys.bys_api.validation.OnCreate;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/offer")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
public class AdminOfferController {

    private final OfferService offerService;
    private final ServiceProviderRepository serviceProviderRepo;

    @PostMapping("/provider/{providerId}")
    public ResponseEntity<OfferDto> create(@PathVariable Long providerId, @Validated(OnCreate.class) @RequestBody OfferDto offerDto) {
        ServiceProvider provider = serviceProviderRepo.findById(providerId).orElseThrow(() -> new EntityNotFoundException("Provider not found"));

        return new ResponseEntity<>(offerService.create(provider, offerDto), HttpStatus.CREATED);
    }

    @GetMapping("/metrics")
    public ResponseEntity<PageDto<OfferMetricsDto>> getAllMetrics(Pageable pageable,
                                                                      @RequestParam(name = "search", required = false) String search,
                                                                      @RequestParam(name = "provider", required = false) List<Long> providerIdList,
                                                                      @RequestParam(name = "user", required = false) List<Long> userIdList,
                                                                      @RequestParam(name = "service_request", required = false) List<Long> serviceRequestIdList,
                                                                      @RequestParam(name = "accepted", required = false) Boolean accepted) {
        return new ResponseEntity<>(offerService.getAllOfferMetrics(pageable, search, providerIdList, serviceRequestIdList, accepted, userIdList), HttpStatus.OK);
    }

    @GetMapping("/metrics/{id}")
    public ResponseEntity<OfferMetricsDto> getMetrics(@PathVariable Long id) {
        return new ResponseEntity<>(offerService.getMetrics(id), HttpStatus.OK);
    }
}
