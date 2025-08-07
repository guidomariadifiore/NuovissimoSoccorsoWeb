package webengineering.nuovissimosoccorsoweb.model;

public interface Abilita {
    int getId();
    void setId(int id);

    TipoAbilita getTipo(); // enum nel DB, enum anche qui
    void setTipo(TipoAbilita tipo);

    int getVersion();
    void setVersion(int version);
}
