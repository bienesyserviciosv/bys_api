package app.bys.bys_api.controller.admin;

import app.bys.bys_api.mapper.PageMapper;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.dto.c2p.C2pAttemptAdminDto;
import app.bys.bys_api.model.enums.C2pAttemptStatus;
import app.bys.bys_api.service.c2p.C2pPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Intentos de cobro C2P (aprobados, rechazados, pendientes y por revisar) para el panel admin. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/c2p")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
public class AdminC2pController {

    private final C2pPaymentService c2pPaymentService;

    @GetMapping("/attempts")
    public ResponseEntity<PageDto<C2pAttemptAdminDto>> getAttempts(
            @RequestParam(name = "status", required = false) C2pAttemptStatus status, Pageable pageable) {
        return ResponseEntity.ok(PageMapper.pageToDto(c2pPaymentService.listAttempts(status, pageable)));
    }
}
