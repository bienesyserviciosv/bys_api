package app.bys.bys_api.model.dto;

import lombok.*;

// Mismo shape que GoogleAuthRequest, adaptado al SDK de Apple: el cliente
// manda "identityToken" (el JWT que Apple firma), no "idToken". givenName/
// familyName son opcionales porque Apple solo los manda la PRIMERA vez que
// el usuario autoriza la app (en logins siguientes vienen null) — se usan
// únicamente para completar el nombre si hace falta crear la cuenta
// (2026-10-04, parte del fix de Guideline 4.8).
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class AppleAuthRequest {

    private String identityToken;
    private String fcmToken;
    private String givenName;
    private String familyName;
}
