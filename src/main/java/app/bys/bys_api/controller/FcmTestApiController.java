package app.bys.bys_api.controller;

import app.bys.bys_api.repository.FinalUserRepository;
import app.bys.bys_api.service.FCMService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/fcm")
@RequiredArgsConstructor
public class FcmTestApiController {

    private final FCMService fcmService;
    private final FinalUserRepository finalUserRepository;

    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getFirebaseConfig() {
        // Firebase configuration for web app
        Map<String, String> config = Map.of(
            "apiKey", "AIzaSyAO58DciUD4yfT4KijeugE-lCD01NCTAhk",
            "authDomain", "bienes-y-servicios-fc46f.firebaseapp.com",
            "projectId", "bienes-y-servicios-fc46f",
            "storageBucket", "bienes-y-servicios-fc46f.firebasestorage.app",
            "messagingSenderId", "862650667877",
            "appId", "1:862650667877:web:94d78c782f9936de39c78c"
        );
        return ResponseEntity.ok(config);
    }

    @PostMapping("/test-notification/{userId}")
    public ResponseEntity<String> sendTestNotification(@PathVariable Long userId) {
        try {
            var user = finalUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            if (user.getFcmToken() == null) {
                return ResponseEntity.badRequest().body("User has no FCM token registered");
            }

            String testTitle = "Test Notification";
            String testBody = "This is a test FCM notification from your app!";
            Map<String, String> testData = Map.of(
                "type", "test",
                "timestamp", String.valueOf(System.currentTimeMillis())
            );

            fcmService.sendNotification(user.getFcmToken(), testTitle, testBody, testData);

            return ResponseEntity.ok("Test notification sent successfully");

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to send notification: " + e.getMessage());
        }
    }
}
