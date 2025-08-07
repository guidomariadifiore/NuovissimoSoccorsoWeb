package webengineering.nuovissimosoccorsoweb.model;

import java.util.List;

public interface User {
    int getId();
    void setId(int id);

    String getNome();
    void setNome(String nome);

    String getCognome();
    void setCognome(String cognome);

    String getEmail();
    void setEmail(String email);

    String getPassword();
    void setPassword(String password);
    
    String getCf();
    void setCf(String cf);

    String getRuolo(); // "Amministratore" o "Operatore"
    void setRuolo(String ruolo);
    
    int getIdCreatore();
    void setIdCreatore(int idCreatore);

    List<Patenti> getPatenti();
    void setPatenti(List<Patenti> patenti);

    List<String> getAbilita(); // descrizioni generiche
    void setAbilita(List<String> abilita);

    int getVersion();
    void setVersion(int version);
}
