package webengineering.nuovissimosoccorsoweb.model;

public interface AggiornaMezzo {
    String getTarga();
    void setTarga(String targa);

    int getIdAmministratore();
    void setIdAmministratore(int idAmministratore);

    String getNota();
    void setNota(String nota);

    int getVersion();
    void setVersion(int version);
}
