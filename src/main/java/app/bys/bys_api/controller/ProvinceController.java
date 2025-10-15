package app.bys.bys_api.controller;

import app.bys.bys_api.model.enums.Province;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/province")
public class ProvinceController {

    @GetMapping
    public ResponseEntity<List<String>> getProvinces() {
        List<String> provinceList = Arrays.stream(Province.values())
                .map(Province::getDisplayName)
                .toList();
        return ResponseEntity.ok(provinceList);
    }
}
