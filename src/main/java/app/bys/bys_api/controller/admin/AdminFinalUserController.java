package app.bys.bys_api.controller.admin;

import app.bys.bys_api.model.dto.FinalUserDto;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.service.FinalUserService;
import app.bys.bys_api.validation.OnCreate;
import app.bys.bys_api.validation.OnUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/final_user")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminFinalUserController {

    private final FinalUserService finalUserService;

    @GetMapping("/{id}")
    public ResponseEntity<FinalUserDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(finalUserService.get(id));
    }

    @GetMapping
    public ResponseEntity<PageDto<FinalUserDto>> getAll(Pageable pageable) {
        return ResponseEntity.ok(finalUserService.getAll(pageable));
    }

    @GetMapping("/role_user")
    public ResponseEntity<PageDto<FinalUserDto>> getAllRoleUser(Pageable pageable) {
        return ResponseEntity.ok(finalUserService.getAllRoleUser(pageable));
    }

    @GetMapping("/role_admin")
    public ResponseEntity<PageDto<FinalUserDto>> getAllRoleAdmin(Pageable pageable) {
        return ResponseEntity.ok(finalUserService.getAllRoleAdmin(pageable));
    }

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
        finalUserService.delete(id);
        return ResponseEntity.ok().build();
    }
}
