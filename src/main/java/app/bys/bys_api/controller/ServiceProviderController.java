package app.bys.bys_api.controller;

import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.dto.ServiceProviderDto;
import app.bys.bys_api.service.ServiceProviderService;
import app.bys.bys_api.validation.OnCreate;
import app.bys.bys_api.validation.OnUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/service_provider")
public class ServiceProviderController {

    private final ServiceProviderService serviceProviderService;

    @GetMapping("/{id}")
    public ResponseEntity<ServiceProviderDto> get(@PathVariable Long id) {
        return new ResponseEntity<>(serviceProviderService.get(id), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<PageDto<ServiceProviderDto>> getAll(Pageable pageable) {
        return new ResponseEntity<>(serviceProviderService.getAll(pageable), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<ServiceProviderDto> create(@Validated(OnCreate.class) @RequestBody ServiceProviderDto serviceProviderDto) {
        return new ResponseEntity<>(serviceProviderService.create(serviceProviderDto), HttpStatus.CREATED);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ServiceProviderDto> update(@PathVariable Long id, @Validated(OnUpdate.class) @RequestBody ServiceProviderDto serviceProviderDto) {
        return new ResponseEntity<>(serviceProviderService.update(id, serviceProviderDto), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        serviceProviderService.delete(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
