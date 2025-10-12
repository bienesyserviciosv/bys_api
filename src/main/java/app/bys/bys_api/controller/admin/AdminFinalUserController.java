package app.bys.bys_api.controller.admin;

import app.bys.bys_api.model.dto.FinalUserMetricsDto;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.service.FinalUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/final_user")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
public class AdminFinalUserController {

    private final FinalUserService finalUserService;

    @GetMapping("/metrics/{id}")
    public ResponseEntity<FinalUserMetricsDto> getUserMetrics(@PathVariable Long id) {
        return ResponseEntity.ok(finalUserService.getUserMetrics(id));
    }

    @GetMapping("/metrics")
    public ResponseEntity<PageDto<FinalUserMetricsDto>> getAllUserMetrics(Pageable pageable,
                                                                          @RequestParam(value = "search", required = false) String search) {
        return ResponseEntity.ok(finalUserService.getAllUserMetrics(pageable, search));
    }

}
