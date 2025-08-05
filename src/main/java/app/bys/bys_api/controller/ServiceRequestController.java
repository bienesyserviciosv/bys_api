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
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<PageDto<ServiceRequestDto>> getAll(Pageable pageable,
                                                             @RequestParam(name = "search", required = false) String search,
                                                             @RequestParam(name = "specializations", required = false) List<Long> specializationList,
                                                             @RequestParam(name = "address", required = false) String address,
                                                             @RequestParam(name = "user", required = false) List<Long> userIdList
    ) {
        return new ResponseEntity<>(serviceRequestService.getAll(pageable, search, specializationList, address, userIdList), HttpStatus.OK);
    }

    //Crear solicitud con el id
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    @PostMapping("/user/{id}")
    public ResponseEntity<ServiceRequestDto> create(@PathVariable Long id, @Validated(OnCreate.class) @RequestBody ServiceRequestDto serviceRequestDto) {
        return new ResponseEntity<>(serviceRequestService.createWithId(id, serviceRequestDto), HttpStatus.CREATED);
    }

    //Crear con authentication
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<ServiceRequestDto> create(Authentication auth, @Validated(OnCreate.class) @RequestBody ServiceRequestDto serviceRequestDto) {
        return new ResponseEntity<>(serviceRequestService.create(auth.getName(), serviceRequestDto), HttpStatus.CREATED);
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
