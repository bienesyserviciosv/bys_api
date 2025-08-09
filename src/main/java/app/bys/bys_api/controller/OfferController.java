package app.bys.bys_api.controller;

import app.bys.bys_api.model.dto.OfferDto;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.service.OfferService;
import app.bys.bys_api.validation.OnCreate;
import app.bys.bys_api.validation.OnUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/offer")
public class OfferController {
    private final OfferService offerService;

    @GetMapping("/{id}")
    public ResponseEntity<OfferDto> get(@PathVariable Long id) {
        return new ResponseEntity<>(offerService.get(id), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<PageDto<OfferDto>> getAll(Pageable pageable,
                                                    @RequestParam(name = "search", required = false) String search,
                                                    @RequestParam(name = "provider", required = false) List<Long> providerIdList) {
        return new ResponseEntity<>(offerService.getAll(pageable, search, providerIdList), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_PROVIDER', 'ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<OfferDto> create(Authentication auth, @Validated(OnCreate.class) @RequestBody OfferDto offerDto) {
        return new ResponseEntity<>(offerService.create(auth.getName(), offerDto), HttpStatus.CREATED);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<OfferDto> update(@PathVariable Long id, @Validated(OnUpdate.class) @RequestBody OfferDto offerDto) {
        return new ResponseEntity<>(offerService.update(id, offerDto), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        offerService.delete(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    @PatchMapping("/accept/{id}")
    public ResponseEntity<OfferDto> acceptOffer(Authentication auth, @PathVariable Long id) {
        return new ResponseEntity<>(offerService.acceptOffer(auth.getName(), id), HttpStatus.OK);
    }
}
