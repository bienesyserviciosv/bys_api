package app.bys.bys_api.controller;

import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.dto.SpecializationDto;
import app.bys.bys_api.service.SpecializationService;
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
@RequestMapping("/specialization")
public class SpecializationController {

    private final SpecializationService specializationService;

    @GetMapping("/{id}")
    public ResponseEntity<SpecializationDto> get(@PathVariable Long id) {
        return new ResponseEntity<>(specializationService.get(id), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<PageDto<SpecializationDto>> getAll(Pageable pageable) {
        return new ResponseEntity<>(specializationService.getAll(pageable), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<SpecializationDto> create(@Validated(OnCreate.class) @RequestBody SpecializationDto specializationDto) {
        return new ResponseEntity<>(specializationService.create(specializationDto), HttpStatus.CREATED);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SpecializationDto> update(@PathVariable Long id, @Validated(OnUpdate.class) @RequestBody SpecializationDto specializationDto) {
        return new ResponseEntity<>(specializationService.update(id, specializationDto), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        specializationService.delete(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
