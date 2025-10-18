package app.bys.bys_api.controller;

import app.bys.bys_api.mapper.ServiceRequestMapper;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.dto.ServiceRequestDto;
import app.bys.bys_api.model.dto.ServiceRequestSummary;
import app.bys.bys_api.model.dto.ServiceRequestWithPictureDto;
import app.bys.bys_api.model.entity.ServiceRequest;
import app.bys.bys_api.service.NotificationService;
import app.bys.bys_api.service.ServiceRequestService;
import app.bys.bys_api.validation.OnCreate;
import app.bys.bys_api.validation.OnUpdate;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/service_request")
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;
    private final NotificationService notificationService;
    private final ServiceRequestMapper serviceRequestMapper;

    @GetMapping("/{id}")
    public ResponseEntity<ServiceRequestSummary> get(@PathVariable Long id) {
        return new ResponseEntity<>(serviceRequestService.get(id), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<PageDto<ServiceRequestSummary>> getAll(Pageable pageable,
                                                                 @RequestParam(name = "search", required = false) String search,
                                                                 @RequestParam(name = "specialization", required = false) List<Long> specializationList,
                                                                 @RequestParam(name = "address", required = false) String address,
                                                                 @RequestParam(name = "user", required = false) List<Long> userIdList,
                                                                 @RequestParam(name = "provider", required = false) List<Long> providerIdList
    ) throws BadRequestException {
        return new ResponseEntity<>(serviceRequestService.getAll(pageable, search, specializationList, address, userIdList, providerIdList), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<ServiceRequestWithPictureDto> create(Authentication auth,
                                                               @Validated(OnCreate.class) @RequestPart(name = "request") ServiceRequestDto serviceRequestDto,
                                                               @RequestPart(name = "pictures", required = false) MultipartFile[] files) {

        ServiceRequest serviceRequest = serviceRequestService.create(auth.getName(), serviceRequestDto, files);

        Long specializationId = serviceRequestDto.getSpecialization().getId();
        notificationService.notifyProvidersOfNewRequest(specializationId, serviceRequest);

        return new ResponseEntity<>(serviceRequestMapper.entityToDtoWithPicture(serviceRequest), HttpStatus.CREATED);
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