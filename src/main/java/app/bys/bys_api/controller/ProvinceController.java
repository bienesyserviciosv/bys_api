package app.bys.bys_api.controller;

import app.bys.bys_api.model.enums.Province;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/province")
public class ProvinceController {

    @GetMapping
    public List<String> getProvinces() {
        return Arrays.stream(Province.values()).map(Enum::name).toList();
    }
}
