package app.bys.bys_api.service;

// OJO: este proyecto fija spring-context-support 5.2.8 y spring-boot-starter-mail
// 2.5.6 en el pom.xml, que usan el paquete javax.mail (no jakarta.mail). Por eso
// estos imports son javax y no jakarta (2026-10-05).
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

@Service
@DependsOn("jwtUtil")
public class OtpService {
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final Map<String, String> otpStorage = new ConcurrentHashMap<>();
    private final Map<String, Integer> resendAttempts = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> otpExpirationTimes = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastResentTimes = new ConcurrentHashMap<>();

    private static final int OTP_EXPIRATION_MINUTES = 5;
    public static final int MAX_RESEND_ATTEMPTS = 3;
    private static final int RESEND_COOLDOWN_MINUTES = 1;

    // Remitente: misma cuenta SMTP configurada en spring.mail.username (hoy
    // Gmail, luego SendPulse con soporte@soytubys.com) — no se duplica acá,
    // se reusa la que ya usa EmailConfig. El nombre que ve el usuario en su
    // bandeja es configurable aparte vía spring.mail.from-name, sin tocar
    // código (2026-10-03).
    // 2026-10-04: con SendPulse el login SMTP (spring.mail.username) NO es
    // la direccion remitente (el login es otro correo), asi que el remitente
    // pasa a ser spring.mail.from-address (env MAIL_FROM). Si no esta
    // definida, cae a spring.mail.username = comportamiento anterior (Gmail).
    @Value("${spring.mail.from-address:${spring.mail.username}}")
    private String fromAddress;

    @Value("${spring.mail.from-name:BYS - Bienes y Servicios}")
    private String fromName;

    // Antes se armaba un SimpleMailMessage de texto plano sin ningún diseño.
    // Ahora se usa el motor de plantillas Thymeleaf que ya estaba configurado
    // en EmailConfig.java (pero sin usar) para mandar el correo en HTML con
    // la identidad visual de BYS (2026-10-03).
    public OtpService(JavaMailSender mailSender,
                       @Qualifier("thymeleafTemplateEngine") SpringTemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    // Genera un OTP de 6 dígitos
    public String generateOTP() {
        SecureRandom random = new SecureRandom();
        return String.format("%06d", random.nextInt(1000000));
    }

    // Envía el OTP por correo, renderizando templates/otp-email.html
    public void sendOTP(String email, String otp) {
        try {
            Context context = new Context();
            context.setVariable("otp", otp);
            context.setVariable("expirationMinutes", OTP_EXPIRATION_MINUTES);
            String htmlBody = templateEngine.process("otp-email", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(email);
            helper.setFrom(fromAddress, fromName);
            helper.setSubject("Tu código de verificación");
            // Primer argumento: alternativa en texto plano para clientes que no
            // renderizan HTML; segundo argumento: el cuerpo HTML real.
            helper.setText("Tu código OTP es: " + otp + ". Válido por " + OTP_EXPIRATION_MINUTES + " minutos.", htmlBody);

            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException("No se pudo enviar el correo de verificación", e);
        }
    }

    // Almacena el OTP (clave: email, valor: OTP)
    public void storeOTP(String email, String otp) {
        otpStorage.put(email, otp);
        // Programa la eliminación después de 5 minutos
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                otpStorage.remove(email);
            }
        }, 5 * 60 * 1000); // 5 minutos
    }

    // Valida el OTP
    public boolean validateOTP(String email, String otp) {
        String storedOTP = otpStorage.get(email);
        return storedOTP != null && storedOTP.equals(otp);
    }

    public void resendOtp(String email) {
        // Verificar si existe un OTP previo
        /*if (!otpStorage.containsKey(email)) {
            throw new RuntimeException("No hay OTP previo para este email");
        }*/

        // Obtener o inicializar contadores de reenvío
        int attempts = resendAttempts.getOrDefault(email, 0);
        LocalDateTime lastResent = lastResentTimes.get(email);

        // Validar límite de reenvíos
        if (attempts >= MAX_RESEND_ATTEMPTS) {
            throw new RuntimeException("You have exceeded the maximum number of OTP resends");
        }

        // Validar tiempo mínimo entre reenvíos
        if (lastResent != null &&
                Duration.between(lastResent, LocalDateTime.now()).toMinutes() < RESEND_COOLDOWN_MINUTES) {
            throw new RuntimeException("You must wait before requesting another OTP");
        }

        // Generar nuevo OTP
        String newOtp = generateOTP();
        otpStorage.put(email, newOtp);

        // Actualizar contadores
        resendAttempts.put(email, attempts + 1);
        lastResentTimes.put(email, LocalDateTime.now());

        // Enviar el OTP
        sendOTP(email, newOtp);
    }

    public int getResendAttempts(String email) {
        return resendAttempts.getOrDefault(email, 0);
    }

    public void restartResendAttempts(String email) {
         resendAttempts.put(email, 0);
    }

    private final Map<String, Boolean> otpVerifiedMap = new ConcurrentHashMap<>();

    public void markOtpVerified(String email) {
        otpVerifiedMap.put(email, true);
    }

    public boolean isOtpVerified(String email) {
        return otpVerifiedMap.getOrDefault(email, false);
    }

    public void clearOtpVerification(String email) {
        otpVerifiedMap.remove(email);
    }
}
