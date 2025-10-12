package app.bys.bys_api.controller.super_admin;

import app.bys.bys_api.model.dto.ServiceProviderDto;
import app.bys.bys_api.model.dto.ServiceProviderWithPictureDto;
import app.bys.bys_api.service.ServiceProviderService;
import app.bys.bys_api.validation.OnCreate;
import app.bys.bys_api.validation.OnUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/super_admin/service_provider")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
public class SuperAdminServiceProviderController {

    private final ServiceProviderService serviceProviderService;

    @PostMapping
    public ResponseEntity<ServiceProviderWithPictureDto> create(@Validated(OnCreate.class)
                                                                @RequestPart(name = "provider") ServiceProviderDto serviceProviderDto,
                                                                @RequestPart(name = "profile_picture", required = false) MultipartFile profilePicture,
                                                                @RequestPart(name = "work_picture_set", required = false) MultipartFile[] workPictureSet) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceProviderService.create(serviceProviderDto, profilePicture, workPictureSet));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ServiceProviderDto> update(@PathVariable Long id, @Validated(OnUpdate.class) @RequestBody ServiceProviderDto serviceProviderDto) {
        return ResponseEntity.ok(serviceProviderService.update(id, serviceProviderDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        serviceProviderService.delete(id);
        return ResponseEntity.ok().build();
    }
}