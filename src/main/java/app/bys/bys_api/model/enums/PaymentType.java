package app.bys.bys_api.model.enums;

public enum PaymentType {
    TRANSFER,
    MOBILE,
    // Cobro en linea por C2P (pago movil con clave OTP) a traves de la API de Bancamiga
    C2P
}
