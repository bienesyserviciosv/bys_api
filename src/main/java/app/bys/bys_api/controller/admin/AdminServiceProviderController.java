package app.bys.bys_api.controller.admin;

import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.dto.ServiceProviderSummary;
import app.bys.bys_api.model.dto.ServiceProviderWithPictureDto;
import app.bys.bys_api.model.enums.MembershipType;
import app.bys.bys_api.service.ServiceProviderService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
@RequestMapping("/admin/service_provider")
public class AdminServiceProviderController {

    private final ServiceProviderService serviceProviderService;

    @GetMapping("/{id}")
    public ResponseEntity<ServiceProviderWithPictureDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(serviceProviderService.get(id));
    }

    @GetMapping
    public ResponseEntity<PageDto<ServiceProviderSummary>> getAll(Pageable pageable,
                                                                  @RequestParam(name = "search", required = false) String search,
                                                                  @RequestParam(name = "specializations", required = false) List<Long> specializationList,
                                                                  @RequestParam(name = "address", required = false) String address,
                                                                  @RequestParam(name = "membershipType", required = false) MembershipType membershipType,
                                                                  @RequestParam(name = "verified", required = false) Boolean verified
                                                                  ) throws BadRequestException {
        return ResponseEntity.ok(serviceProviderService.getAll(pageable, search, specializationList, address, membershipType, verified));
    }
}
