package app.bys.bys_api.controller;

import app.bys.bys_api.model.dto.FinalUserDto;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.service.FinalUserService;
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
@RequestMapping("/final_user")
public class FinalUserController {

    private final FinalUserService finalUserService;

    @GetMapping("/{id}")
    public ResponseEntity<FinalUserDto> get(@PathVariable Long id) {
        return new ResponseEntity<>(finalUserService.get(id), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<PageDto<FinalUserDto>> getAll(Pageable pageable) {
        return new ResponseEntity<>(finalUserService.getAll(pageable), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<FinalUserDto> create(@Validated(OnCreate.class) @RequestBody FinalUserDto finalUserDto) {
        return new ResponseEntity<>(finalUserService.create(finalUserDto), HttpStatus.CREATED);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<FinalUserDto> update(@PathVariable Long id, @Validated(OnUpdate.class) @RequestBody FinalUserDto finalUserDto) {
        return new ResponseEntity<>(finalUserService.update(id, finalUserDto), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        finalUserService.delete(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
