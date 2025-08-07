package webengineering.nuovissimosoccorsoweb.model;

public interface Materiale {
    int getId();
    void setId(int id);

    String getNome();
    void setNome(String nome);

    String getDescrizione();
    void setDescrizione(String descrizione);

    int getVersion();
    void setVersion(int version);
} 
