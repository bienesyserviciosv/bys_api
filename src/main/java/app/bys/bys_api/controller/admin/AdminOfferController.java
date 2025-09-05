package app.bys.bys_api.controller.admin;

import app.bys.bys_api.model.dto.OfferDto;
import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.repository.ServiceProviderRepository;
import app.bys.bys_api.service.OfferService;
import app.bys.bys_api.validation.OnCreate;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/offer")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminOfferController {

    private final OfferService offerService;
    private final ServiceProviderRepository serviceProviderRepo;

    @PostMapping("/provider/{providerId}")
    public ResponseEntity<OfferDto> create(@PathVariable Long providerId, @Validated(OnCreate.class) @RequestBody OfferDto offerDto) {
        ServiceProvider provider = serviceProviderRepo.findById(providerId).orElseThrow(() -> new EntityNotFoundException("Provider not found"));

        return new ResponseEntity<>(offerService.create(provider, offerDto), HttpStatus.CREATED);
    }
}
