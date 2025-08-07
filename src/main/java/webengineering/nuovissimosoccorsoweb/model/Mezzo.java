package webengineering.nuovissimosoccorsoweb.model;

public interface Mezzo {
    String getTarga();
    void setTarga(String targa);

    String getNome();
    void setNome(String nome);

    String getDescrizione();
    void setDescrizione(String descrizione);

    int getVersion();
    void setVersion(int version);
}
