package app.bys.bys_api.controller.super_admin;

import app.bys.bys_api.model.dto.FinalUserDto;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.service.SuperAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/super_admin")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    @GetMapping("/me")
    public ResponseEntity<FinalUserDto> getSuperAdmin() {
        return ResponseEntity.ok(superAdminService.getSuperAdmin());
    }

    @GetMapping
    public ResponseEntity<PageDto<FinalUserDto>> getAllAdmin(Pageable pageable) {
        return ResponseEntity.ok(superAdminService.getAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FinalUserDto> getAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(superAdminService.getAdmin(id));
    }

    @PostMapping
    public ResponseEntity<FinalUserDto> addAdmin(@RequestBody FinalUserDto finalUserDto) {
        return ResponseEntity.ok(superAdminService.create(finalUserDto));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<FinalUserDto> updateAdmin(@PathVariable Long id, @RequestBody FinalUserDto finalUserDto) {
        return ResponseEntity.ok(superAdminService.update(id, finalUserDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAdmin(@PathVariable Long id) {
        superAdminService.delete(id);
        return ResponseEntity.ok().build();
    }

}
