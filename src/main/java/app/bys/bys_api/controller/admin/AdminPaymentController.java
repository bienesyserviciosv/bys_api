package app.bys.bys_api.controller.admin;

import app.bys.bys_api.mapper.ServiceRequestMapper;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.dto.ServiceRequestDto;
import app.bys.bys_api.model.entity.Payment;
import app.bys.bys_api.model.entity.ServiceRequest;
import app.bys.bys_api.repository.ServiceRequestRepository;
import app.bys.bys_api.service.NotificationService;
import app.bys.bys_api.service.PaymentService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/payment")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
public class AdminPaymentController {

    private final ServiceRequestRepository serviceRequestRepository;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final ServiceRequestMapper serviceRequestMapper;

    @GetMapping
    public ResponseEntity<PageDto<Object>> getAll(Pageable pageable,
                                                      @RequestParam(name = "search", required = false) String search,
                                                      @RequestParam(name = "user", required = false) List<Long> userIdList,
                                                      @RequestParam(name = "provider", required = false) List<Long> providerIdList) throws BadRequestException {
        return ResponseEntity.ok(paymentService.getAllPayments(pageable, search, userIdList, providerIdList));
    }

    @PatchMapping("/accept/{id}")
    public ResponseEntity<ServiceRequestDto> accept(@PathVariable Long id) {
        Payment payment = paymentService.acceptPayment(id);
        Long requestId = payment.getOffer().getServiceRequest().getId();
        Long userId = payment.getFinalUser().getId();
        Long providerId = payment.getServiceProvider().getId();
        notificationService.notifyPaymentAccepted(userId, providerId, requestId);

        ServiceRequest serviceRequest = serviceRequestRepository.findById(requestId)
                .orElseThrow(()-> new EntityNotFoundException("Service request not found"));

        return ResponseEntity.ok(serviceRequestMapper.entityToDto(serviceRequest));
    }

    @PatchMapping("/reject/{id}")
    public ResponseEntity<ServiceRequestDto> reject(@PathVariable Long id) {
        Payment payment = paymentService.rejectPayment(id);

        Long requestId = payment.getOffer().getServiceRequest().getId();

        //Agregar Metodo de notificaciones para avisar al usuario de que se rechazó su pago
        ServiceRequest serviceRequest = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Service request not found"));

        return ResponseEntity.ok(serviceRequestMapper.entityToDto(serviceRequest));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable Long id) {
        paymentService.delete(id);
        return ResponseEntity.ok().build();
    }
}
