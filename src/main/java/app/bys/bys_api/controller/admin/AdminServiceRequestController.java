package app.bys.bys_api.controller.admin;

import app.bys.bys_api.mapper.ServiceRequestMapper;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.dto.ServiceRequestDto;
import app.bys.bys_api.model.dto.ServiceRequestMetricsDto;
import app.bys.bys_api.model.dto.ServiceRequestWithPictureDto;
import app.bys.bys_api.model.entity.ServiceRequest;
import app.bys.bys_api.service.NotificationService;
import app.bys.bys_api.service.ServiceRequestService;
import app.bys.bys_api.validation.OnCreate;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
@RequestMapping("/admin/service_request")
public class AdminServiceRequestController {

    private final ServiceRequestService serviceRequestService;
    private final NotificationService notificationService;
    private final ServiceRequestMapper serviceRequestMapper;

    @GetMapping("/metrics/{id}")
    public ResponseEntity<ServiceRequestMetricsDto> get(@PathVariable Long id) {
        return new ResponseEntity<>(serviceRequestService.getRequestMetrics(id), HttpStatus.OK);
    }

    @GetMapping("/metrics")
    public ResponseEntity<PageDto<ServiceRequestMetricsDto>> getAllRequestMetrics(Pageable pageable,
                                                                                  @RequestParam(name = "search", required = false) String search,
                                                                                  @RequestParam(name = "specialization", required = false) List<Long> specializationList,
                                                                                  @RequestParam(name = "address", required = false) String address,
                                                                                  @RequestParam(name = "user", required = false) List<Long> userIdList
                                                                                  ) throws BadRequestException {
        return new ResponseEntity<>(serviceRequestService.getAllRequestMetrics(pageable, search, specializationList, address, userIdList), HttpStatus.OK);
    }

    @PostMapping("/user/{id}")
    public ResponseEntity<ServiceRequestWithPictureDto> create(@PathVariable Long id,
                                                                      @Validated(OnCreate.class) @RequestPart(name = "request") ServiceRequestDto serviceRequestDto,
                                                                      @RequestPart(name = "pictures", required = false) MultipartFile[] files) {

        ServiceRequest serviceRequest = serviceRequestService.createWithUserId(id, serviceRequestDto, files);

        Long specializationId = serviceRequestDto.getSpecialization().getId();
        notificationService.notifyProvidersOfNewRequest(specializationId, serviceRequestDto.getAddress(), serviceRequest);

        return new ResponseEntity<>(serviceRequestMapper.entityToDtoWithPicture(serviceRequest), HttpStatus.CREATED);
    }
}
