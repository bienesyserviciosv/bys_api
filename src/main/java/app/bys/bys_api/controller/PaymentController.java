package app.bys.bys_api.controller;

import app.bys.bys_api.model.dto.MobilePaymentDto;
import app.bys.bys_api.model.dto.TransferPaymentDto;
import app.bys.bys_api.model.enums.BankName;
import app.bys.bys_api.model.enums.PhoneCode;
import app.bys.bys_api.service.PaymentService;
import app.bys.bys_api.validation.OnCreate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payment")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/{id}")
    public ResponseEntity<Object> getPayment(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPayment(id));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    @PostMapping("/mobile")
    public ResponseEntity<MobilePaymentDto> createMobilePayment(
            @Validated(OnCreate.class) @RequestPart(name = "payment") MobilePaymentDto mobilePaymentDto,
            @RequestPart(name = "screenshot") MultipartFile picture) {

        MobilePaymentDto paymentDto = paymentService.createMobilePayment(mobilePaymentDto, picture);
        //notificationService.notifyAdminsOfNewPayment(paymentDto.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentDto);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    @PostMapping("/transfer")
    public ResponseEntity<TransferPaymentDto> createTransferPayment(
            @Validated(OnCreate.class) @RequestPart(name = "payment") TransferPaymentDto transferPaymentDto,
            @RequestPart(name = "screenshot") MultipartFile picture) {
        TransferPaymentDto paymentDto = paymentService.createTransferPayment(transferPaymentDto, picture);
        //notificationService.notifyAdminsOfNewPayment(paymentDto.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentDto);
    }

    @PatchMapping("/mobile/{id}")
    public ResponseEntity<MobilePaymentDto> updateMobilePayment(@PathVariable Long id, @RequestBody MobilePaymentDto mobilePaymentDto) {
        return ResponseEntity.ok(paymentService.updateMobilePayment(id, mobilePaymentDto));
    }

    @PatchMapping("/transfer/{id}")
    public ResponseEntity<TransferPaymentDto> updateTransferPayment(@PathVariable Long id, @RequestBody TransferPaymentDto transferPaymentDto) {
        return ResponseEntity.ok(paymentService.updateTransferPayment(id, transferPaymentDto));
    }

    @GetMapping("/banks")
    public ResponseEntity<List<String>> getAvailableBanks() {
        List<String> banks = Arrays.stream(BankName.values())
                .map(BankName::getDisplayName)
                .collect(Collectors.toList());
        return ResponseEntity.ok(banks);
    }

    @GetMapping("/phone_codes")
    public ResponseEntity<List<String>> getPhoneCodes() {
        List<String> banks = Arrays.stream(PhoneCode.values())
                .map(PhoneCode::getCode)
                .collect(Collectors.toList());
        return ResponseEntity.ok(banks);
    }


}
