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

//    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
//    @GetMapping("/admin/{id}")
//    public ResponseEntity<FinalUserDto> get(@PathVariable Long id) {
//        return new ResponseEntity<>(finalUserService.get(id), HttpStatus.OK);
//    }
//
//    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
//    @GetMapping({"/me"})
//    public ResponseEntity<FinalUserDto> getOwnProfile(Authentication authentication) {
//        return new ResponseEntity<>(finalUserService.getWithEmail(authentication.getName()), HttpStatus.OK);
//    }
//
//    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
//    @GetMapping
//    public ResponseEntity<PageDto<FinalUserDto>> getAll(Pageable pageable) {
//        return new ResponseEntity<>(finalUserService.getAll(pageable), HttpStatus.OK);
//    }
//
//    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
//    @PostMapping
//    public ResponseEntity<FinalUserDto> create(@Validated(OnCreate.class) @RequestBody FinalUserDto finalUserDto) {
//        return new ResponseEntity<>(finalUserService.create(finalUserDto), HttpStatus.CREATED);
//    }
//
//    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
//    @PatchMapping("/{id}")
//    public ResponseEntity<FinalUserDto> update(@PathVariable Long id, @Validated(OnUpdate.class) @RequestBody FinalUserDto finalUserDto) {
//        return new ResponseEntity<>(finalUserService.update(id, finalUserDto), HttpStatus.OK);
//    }
//
//    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
//    @PatchMapping("/me")
//    public ResponseEntity<FinalUserDto> updateOwnProfile(Authentication authentication, @Validated(OnUpdate.class) @RequestBody FinalUserDto finalUserDto) {
//        return new ResponseEntity<>(finalUserService.updateByEmail(authentication.getName(), finalUserDto), HttpStatus.OK);
//    }
//
//    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> delete(@PathVariable Long id) {
//        finalUserService.delete(id);
//        return new ResponseEntity<>(HttpStatus.OK);
//    }
//
//    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
//    @DeleteMapping("/me")
//    public ResponseEntity<Void> deleteOwnProfile(Authentication authentication) {
//        finalUserService.deleteByEmail(authentication.getName());
//        return new ResponseEntity<>(HttpStatus.OK);
//    }
//
//}
