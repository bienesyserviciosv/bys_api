package app.bys.bys_api.service;

import org.springframework.context.annotation.DependsOn;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

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
    private final Map<String, String> otpStorage = new ConcurrentHashMap<>();
    private final Map<String, Integer> resendAttempts = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> otpExpirationTimes = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastResentTimes = new ConcurrentHashMap<>();

    private static final int OTP_EXPIRATION_MINUTES = 5;
    public static final int MAX_RESEND_ATTEMPTS = 3;
    private static final int RESEND_COOLDOWN_MINUTES = 1;

     public OtpService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // Genera un OTP de 6 dígitos
    public String generateOTP() {
        SecureRandom random = new SecureRandom();
        return String.format("%06d", random.nextInt(1000000));
    }

    // Envía el OTP por correo
    public void sendOTP(String email, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Tu código de verificación");
        message.setText("Tu código OTP es: " + otp + ". Válido por 5 minutos.");
        mailSender.send(message);
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
