package app.bys.bys_api.controller.super_admin;

import app.bys.bys_api.model.dto.FinalUserDto;
import app.bys.bys_api.service.AccountDeletionService;
import app.bys.bys_api.service.FinalUserService;
import app.bys.bys_api.validation.OnCreate;
import app.bys.bys_api.validation.OnUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/super_admin/final_user")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
public class SuperAdminFinalUserController {

    private final FinalUserService finalUserService;
    private final AccountDeletionService accountDeletionService;

    @PostMapping
    public ResponseEntity<FinalUserDto> create(@Validated(OnCreate.class) @RequestBody FinalUserDto finalUserDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(finalUserService.create(finalUserDto));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<FinalUserDto> update(@PathVariable Long id, @Validated(OnUpdate.class) @RequestBody FinalUserDto finalUserDto) {
        return ResponseEntity.ok(finalUserService.update(id, finalUserDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        // 2026-10-05: borrado completo (solicitudes, ofertas, pagos, etc.), ver AccountDeletionService.
        accountDeletionService.deleteClient(id);
        return ResponseEntity.ok().build();
    }
}
