package app.bys.bys_api.controller.admin;

import app.bys.bys_api.model.dto.GlobalStatsDto;
import app.bys.bys_api.service.GlobalStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequestMapping("/admin/global_stats")
public class AdminGlobalStatsController {

    private final GlobalStatsService globalStatsService;

    @GetMapping
    public ResponseEntity<GlobalStatsDto> getStats(){
        return new ResponseEntity<>(globalStatsService.getGlobalStats(), HttpStatus.OK);
    }
}
