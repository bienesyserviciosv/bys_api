package app.bys.bys_api.controller;

import app.bys.bys_api.model.dto.NotificationDto;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notification")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<PageDto<NotificationDto>> getAll(Pageable pageable,
                                                           @RequestParam(name = "search", required = false) String search,
                                                           @RequestParam(name = "provider", required = false) List<Long> providerIdList,
                                                           @RequestParam(name = "user", required = false) List<Long> finalUserIdList) {
        return new ResponseEntity<>(notificationService.getAllNotifications(pageable, search, providerIdList, finalUserIdList), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationDto> get(@PathVariable Long id) {
        return new ResponseEntity<>(notificationService.getById(id), HttpStatus.OK);
    }

    @GetMapping("/read/{id}")
    public ResponseEntity<NotificationDto> read(@PathVariable Long id, Authentication auth) {
        return new ResponseEntity<>(notificationService.readNotification(id, auth.getName()), HttpStatus.OK);
    }
}
