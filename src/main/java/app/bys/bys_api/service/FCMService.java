package app.bys.bys_api.service;

import app.bys.bys_api.model.dto.NotificationRequest;
import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class FCMService {

    private final FirebaseMessaging firebaseMessaging;

    public FCMService(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    public void sendNotification(String token, String title, String body, Map<String, String> dataPayload)
            throws FirebaseMessagingException {

        Message.Builder message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build());

        if (dataPayload != null) {
            message.putAllData(dataPayload);
        }

        log.info("FCM → token: {}", token);
        log.info("FCM → title: {}", title);
        log.info("FCM → body: {}", body);
        log.info("FCM → data: {}", dataPayload);

        firebaseMessaging.send(message.build());
    }

    /**
     * Enviar múltiples notificaciones en batch.
     */
    public BatchResponse sendBatchNotifications(List<NotificationRequest> requests) throws FirebaseMessagingException {
        List<Message> messages = new ArrayList<>();

        for (NotificationRequest req : requests) {
            Message.Builder builder = Message.builder()
                    .setToken(req.token())
                    .setNotification(Notification.builder()
                            .setTitle(req.title())
                            .setBody(req.body())
                            .build());

            if (req.dataPayload() != null) {
                builder.putAllData(req.dataPayload());
            }

            messages.add(builder.build());
            log.info("Notification request: {}", req);
        }

        return firebaseMessaging.sendEach(messages);
    }

        /**
         * Envía una notificación a un Tópico específico de FCM.
         */
        public void sendTopicNotification(String topic, String title, String body, Map<String, String> data)
                throws FirebaseMessagingException {

            // 1. Construir la Notificación (lo que ve el usuario)
            com.google.firebase.messaging.Notification notification =
                    com.google.firebase.messaging.Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build();

            // 2. Construir el Mensaje FCM
            Message message = Message.builder()
                    .setNotification(notification)
                    .putAllData(data)
                    .setTopic(topic)
                    .build();

            // 3. Enviar el mensaje
           firebaseMessaging.send(message);

            log.info("Notificación enviada con éxito al tópico {}.", topic);
        }
    }

