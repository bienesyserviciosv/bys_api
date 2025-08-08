package app.bys.bys_api.controller;

import app.bys.bys_api.model.dto.NotificationDto;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notification")
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<PageDto<NotificationDto>> getAll(Pageable pageable,
                                                           @RequestParam(name = "search", required = false) String search,
                                                           @RequestParam(name = "provider", required = false) List<Long> providerIdList) {
        return new ResponseEntity<>(notificationService.getNotifications(pageable, search, providerIdList), HttpStatus.OK);
    }
}
