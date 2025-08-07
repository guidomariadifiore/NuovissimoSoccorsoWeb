package webengineering.nuovissimosoccorsoweb.model;

public enum TipoPatente {
    AM, A1, A2, A, B1, B, B96, BE,
    C1, C1E, C, CE, D1, D1E, D, DE,
    KA, KB, CQC, CQC_Merci;

    public static TipoPatente fromString(String s) {
        return TipoPatente.valueOf(s.replace(" ", "_"));
    }

    public String toDBString() {
        return this.name().replace("_", " ");
    }
}
