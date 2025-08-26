package app.bys.bys_api.controller;

import app.bys.bys_api.mapper.ServiceRequestMapper;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.dto.ServiceRequestDto;
import app.bys.bys_api.model.entity.ServiceRequest;
import app.bys.bys_api.repository.ServiceRequestRepository;
import app.bys.bys_api.service.NotificationService;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/service_request")
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;
    private final NotificationService notificationService;
    private final ServiceRequestRepository serviceRequestRepository;
    private final ServiceRequestMapper serviceRequestMapper;

    @GetMapping("/{id}")
    public ResponseEntity<ServiceRequestDto> get(@PathVariable Long id) {
        return new ResponseEntity<>(serviceRequestService.get(id), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<PageDto<ServiceRequestDto>> getAll(Pageable pageable,
                                                             @RequestParam(name = "search", required = false) String search,
                                                             @RequestParam(name = "specialization", required = false) List<Long> specializationList,
                                                             @RequestParam(name = "address", required = false) String address,
                                                             @RequestParam(name = "user", required = false) List<Long> userIdList
    ) {
        return new ResponseEntity<>(serviceRequestService.getAll(pageable, search, specializationList, address, userIdList), HttpStatus.OK);
    }

    //Crear solicitud con el id
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    @PostMapping("/user/{id}")
    public ResponseEntity<ServiceRequestDto> create(@PathVariable Long id, @Validated(OnCreate.class) @RequestBody ServiceRequestDto serviceRequestDto) {

        ServiceRequest serviceRequest = serviceRequestService.createWithId(id, serviceRequestDto);

        Long specializationId = serviceRequestDto.getSpecialization().getId();
        notificationService.notifyProviders(specializationId, serviceRequestDto.getAddress(), serviceRequest);

        return new ResponseEntity<>(serviceRequestMapper.entityToDto(serviceRequest), HttpStatus.CREATED);
    }

    //Crear con authentication
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<ServiceRequestDto> create(Authentication auth, @Validated(OnCreate.class) @RequestBody ServiceRequestDto serviceRequestDto) {
        ServiceRequest serviceRequest = serviceRequestService.create(auth.getName(), serviceRequestDto);

        Long specializationId = serviceRequestDto.getSpecialization().getId();
        notificationService.notifyProviders(specializationId, serviceRequestDto.getAddress(), serviceRequest);

        return new ResponseEntity<>(serviceRequestMapper.entityToDto(serviceRequest), HttpStatus.CREATED);
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

/*  public ResponseEntity<ServiceRequestDto> create(@PathVariable Long id, @Validated(OnCreate.class) @RequestBody ServiceRequestDto serviceRequestDto) {

        ServiceRequest serviceRequest = serviceRequestService.createWithId(id, serviceRequestDto);

        Long specializationId = serviceRequestDto.getSpecialization().getId();
        notificationService.notifyProviders(specializationId, serviceRequestDto.getAddress(), serviceRequest);

        ServiceRequestDto responseDto = serviceRequestMapper.entityToDto(serviceRequest);

//        Map<String, Object> response = new HashMap<>();
//        response.put("Service request", requestDto);
//        response.put("message", message);

        //return ResponseEntity.status(HttpStatus.CREATED).body(response);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }*/