package app.bys.bys_api.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum BankName {
    BANK_100_PORCIENTO("100% Banco", null),
    BANK_BANCAMIGA("Bancamiga", "0172"),
    BANK_BANCARIBE("BanCaribe", null),
    BANK_ACTIVO("Banco Activo", null),
    BANK_DIGITAL_TRABAJADORES("Banco Digital de los Trabajadores", null),
    BANK_CARONI("Banco Caroní", null),
    BANK_VENEZUELA("Banco de Venezuela", null),
    BANK_TESORO("Banco del Tesoro", null),
    BANK_EXTERIOR("Banco Exterior", null),
    BANK_MERCANTIL("Banco Mercantil", null),
    BANK_NACIONAL_CREDITO("Banco Nacional de Crédito (BNC)", null),
    BANK_PLAZA("Banco Plaza", null),
    BANK_SOFITASA("Banco Sofitasa", null),
    BANK_VENEZOLANO_CREDITO("Banco Venezolano de Credito", null),
    BANK_BANCRECER("Bancrecer", null),
    BANK_BANESCO("Banesco", null),
    BANK_BANFANB("BANFANB", null),
    BANK_BANPLUS("Banplus", null),
    BANK_BBVA_PROVINCIAL("BBVA Provincial", null),
    BANK_BFC("BFC Banco Fondo Común", null),
    BANK_DELSUR("DELSUR", null),
    BANK_R4("R4", null),
    BANK_BANGENTE("Bangente", null),;

    @JsonValue
    private final String displayName;

    // Código bancario (SUDEBAN) que espera la API de Bancamiga en "Bank"/"BancoOrig". Pendiente de confirmar el resto del catálogo con Bancamiga.
    private final String bancamigaCode;

    BankName(String displayName, String bancamigaCode) {
        this.displayName = displayName;
        this.bancamigaCode = bancamigaCode;
    }

    public static BankName fromBancamigaCode(String code) {
        return Arrays.stream(BankName.values())
                .filter(b -> b.bancamigaCode != null && b.bancamigaCode.equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }

    public static BankName fromDisplayName(String input) {
        return Arrays.stream(BankName.values())
                .filter(b -> b.displayName.equalsIgnoreCase(input))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid bank name: " + input));
    }


}
