package webengineering.nuovissimosoccorsoweb.model;

public interface Operatore extends User {
    int getIdAmministratore();
    void setIdAmministratore(int idAmministratore);
    
    String getCodiceFiscale();
    void setCodiceFiscale(String codiceFiscale);
}
