package app.bys.bys_api.controller;

import app.bys.bys_api.model.dto.FinalUserDto;
import app.bys.bys_api.service.FinalUserService;
import app.bys.bys_api.validation.OnUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/final_user")
@PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
public class FinalUserController {

    private final FinalUserService finalUserService;

    @PreAuthorize("hasAnyAuthority('ROLE_PROVIDER', 'ROLE_USER', 'ROLE_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<FinalUserDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(finalUserService.get(id));
    }

    @GetMapping("/me")
    public ResponseEntity<FinalUserDto> getOwnProfile(Authentication authentication) {
        return ResponseEntity.ok(finalUserService.getWithEmail(authentication.getName()));
    }

    @PatchMapping("/me")
    public ResponseEntity<FinalUserDto> updateOwnProfile(
            Authentication authentication,
            @Validated(OnUpdate.class) @RequestBody FinalUserDto finalUserDto) {
        return ResponseEntity.ok(finalUserService.updateByEmail(authentication.getName(), finalUserDto));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteOwnProfile(Authentication authentication) {
        finalUserService.deleteByEmail(authentication.getName());
        return ResponseEntity.ok().build();
    }
}

