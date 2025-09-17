package app.bys.bys_api.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum BankName {
    BANK_100_PORCIENTO("100% Banco"),
    BANK_BANCAMIGA("Bancamiga"),
    BANK_BANCARIBE("BanCaribe"),
    BANK_ACTIVO("Banco Activo"),
    BANK_DIGITAL_TRABAJADORES("Banco Digital de los Trabajadores"),
    BANK_CARONI("Banco Caroní"),
    BANK_VENEZUELA("Banco de Venezuela"),
    BANK_TESORO("Banco del Tesoro"),
    BANK_EXTERIOR_MERCANTIL("Banco Exterior Mercantil"),
    BANK_NACIONAL_CREDITO("Banco Nacional de Crédito (BNC)"),
    BANK_PLAZA("Banco Plaza"),
    BANK_SOFITASA("Banco Sofitasa"),
    BANK_VENEZOLANO_CREDITO("Banco Venzolano de Credito"),
    BANK_BANCRECER("Bancrecer"),
    BANK_BANESCO("Banesco"),
    BANK_BANFANB("BANFANB"),
    BANK_BANPLUS("Banplus"),
    BANK_BBVA_PROVINCIAL("BBVA Provincial"),
    BANK_BFC("BFC Banco Fondo Común"),
    BANK_DELSUR("DELSUR"),
    BANK_R4("R4"),
    BANK_BANGENTE("Bangente"),;

    @JsonValue
    private final String displayName;

    BankName(String displayName) {
        this.displayName = displayName;
    }

}
