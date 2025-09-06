package app.bys.bys_api.controller;

import app.bys.bys_api.model.dto.ServiceProviderDto;
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
    public ResponseEntity<ServiceProviderDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(serviceProviderService.get(id));
    }

    @GetMapping("/me")
    public ResponseEntity<ServiceProviderDto> getOwnProfile(Authentication authentication) {
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

//    private final ServiceProviderService serviceProviderService;
//
//    @GetMapping("/{id}")
//    public ResponseEntity<ServiceProviderDto> get(@PathVariable Long id) {
//        return new ResponseEntity<>(serviceProviderService.get(id), HttpStatus.OK);
//    }
//
//    @GetMapping
//    public ResponseEntity<PageDto<ServiceProviderDto>> getAll(Pageable pageable,
//                                                              @RequestParam(name = "search", required = false) String search,
//                                                              @RequestParam(name = "specializations", required = false) List<Long> specializationList,
//                                                              @RequestParam(name = "address", required = false) String address
//    ) {
//        return new ResponseEntity<>(serviceProviderService.getAll(pageable, search, specializationList, address), HttpStatus.OK);
//    }
//
//    @PostMapping
//    public ResponseEntity<ServiceProviderDto> create(@Validated(OnCreate.class) @RequestBody ServiceProviderDto serviceProviderDto) {
//        return new ResponseEntity<>(serviceProviderService.create(serviceProviderDto), HttpStatus.CREATED);
//    }
//
//    @PatchMapping("/{id}")
//    public ResponseEntity<ServiceProviderDto> update(@PathVariable Long id, @Validated(OnUpdate.class) @RequestBody ServiceProviderDto serviceProviderDto) {
//        return new ResponseEntity<>(serviceProviderService.update(id, serviceProviderDto), HttpStatus.OK);
//    }
//
//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> delete(@PathVariable Long id) {
//        serviceProviderService.delete(id);
//        return new ResponseEntity<>(HttpStatus.OK);
//    }
}
