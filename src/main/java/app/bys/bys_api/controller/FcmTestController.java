package app.bys.bys_api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/fcm-test")
public class FcmTestController {

    @GetMapping
    public String showFcmTestPage() {
        return "fcm-test";
    }
}
