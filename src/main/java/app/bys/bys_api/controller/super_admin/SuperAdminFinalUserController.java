package app.bys.bys_api.controller.super_admin;

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
@RequestMapping("/super_admin/final_user")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
public class SuperAdminFinalUserController {

    private final FinalUserService finalUserService;

    @GetMapping("/{id}")
    public ResponseEntity<FinalUserDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(finalUserService.get(id));
    }

    @GetMapping
    public ResponseEntity<PageDto<FinalUserDto>> getAll(Pageable pageable,
                                                        @RequestParam(value = "search", required = false) String search) {
        return ResponseEntity.ok(finalUserService.getAll(pageable, search));
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
