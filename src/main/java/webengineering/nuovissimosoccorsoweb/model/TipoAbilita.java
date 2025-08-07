package webengineering.nuovissimosoccorsoweb.model;

public enum TipoAbilita {
    Pompiere,
    Medico,
    Infermiere,
    Elettricista,
    Sommozzatore,
    Alpinista,
    Tecnico_di_primo_soccorso,
    Meccanico,
    Logistica_e_coordinamento,
    Pilota_di_droni;

    public static TipoAbilita fromString(String value) {
        return TipoAbilita.valueOf(value.replace(" ", "_").replace("-", "_"));
    }

    public String toDBString() {
        return this.name().replace("_", " ");
    }
} 
