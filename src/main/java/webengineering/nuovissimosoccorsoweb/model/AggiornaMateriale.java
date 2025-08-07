package webengineering.nuovissimosoccorsoweb.model;

public interface AggiornaMateriale {
    int getIdMateriale();
    void setIdMateriale(int idMateriale);

    int getIdAmministratore();
    void setIdAmministratore(int idAmministratore);

    String getNota();
    void setNota(String nota);

    int getVersion();
    void setVersion(int version);
}
