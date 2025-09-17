package app.bys.bys_api.controller;

import app.bys.bys_api.model.dto.ServiceProviderDto;
import app.bys.bys_api.model.dto.ServiceProviderWithPictureDto;
import app.bys.bys_api.service.ServiceProviderService;
import app.bys.bys_api.validation.OnUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/service_provider")
@PreAuthorize("hasAnyAuthority('ROLE_PROVIDER')")
public class ServiceProviderController {

    private final ServiceProviderService serviceProviderService;

    @PreAuthorize("hasAnyAuthority('ROLE_PROVIDER', 'ROLE_USER', 'ROLE_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ServiceProviderWithPictureDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(serviceProviderService.get(id));
    }

    @GetMapping("/me")
    public ResponseEntity<ServiceProviderWithPictureDto> getOwnProfile(Authentication authentication) {
        return ResponseEntity.ok(serviceProviderService.getWithEmail(authentication.getName()));
    }

    @PatchMapping("/me")
    public ResponseEntity<ServiceProviderDto> updateOwnProfile(
            Authentication authentication,
            @Validated(OnUpdate.class) @RequestBody ServiceProviderDto serviceProviderDto) {
        return ResponseEntity.ok(serviceProviderService.updateByEmail(authentication.getName(), serviceProviderDto));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteOwnProfile(Authentication authentication) {
        serviceProviderService.deleteByEmail(authentication.getName());
        return ResponseEntity.ok().build();
    }
}
