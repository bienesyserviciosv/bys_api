package app.bys.bys_api.controller;

import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.dto.ServiceRequestDto;
import app.bys.bys_api.service.ServiceRequestService;
import app.bys.bys_api.validation.OnCreate;
import app.bys.bys_api.validation.OnUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/service_request")
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;

    @GetMapping("/{id}")
    public ResponseEntity<ServiceRequestDto> get(@PathVariable Long id) {
        return new ResponseEntity<>(serviceRequestService.get(id), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<PageDto<ServiceRequestDto>> getAll(Pageable pageable) {
        return new ResponseEntity<>(serviceRequestService.getAll(pageable), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    @PostMapping("/user/{id}")
    public ResponseEntity<ServiceRequestDto> create(@PathVariable Long id, @Validated(OnCreate.class) @RequestBody ServiceRequestDto serviceRequestDto) {
        return new ResponseEntity<>(serviceRequestService.create(id, serviceRequestDto), HttpStatus.CREATED);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ServiceRequestDto> update(@PathVariable Long id, @Validated(OnUpdate.class) @RequestBody ServiceRequestDto serviceRequestDto) {
        return new ResponseEntity<>(serviceRequestService.update(id, serviceRequestDto), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        serviceRequestService.delete(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
