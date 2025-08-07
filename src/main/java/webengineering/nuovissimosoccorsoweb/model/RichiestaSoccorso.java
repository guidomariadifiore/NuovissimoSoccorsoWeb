package webengineering.nuovissimosoccorsoweb.model;

public interface RichiestaSoccorso {

    int getCodice();
    void setCodice(int codice);

    String getStato(); // Inviata, Convalidata, Attiva, Annullata, Chiusa
    void setStato(String stato);

    String getCoordinate();
    void setCoordinate(String coordinate);

    String getIndirizzo();
    void setIndirizzo(String indirizzo);

    String getDescrizione();
    void setDescrizione(String descrizione);

    String getStringa();
    void setStringa(String stringa); // usata per validazione email

    String getNome();
    void setNome(String nome);

    String getFoto();
    void setFoto(String foto);

    String getIp();
    void setIp(String ip);

    String getEmailSegnalante();
    void setEmailSegnalante(String email);

    String getNomeSegnalante();
    void setNomeSegnalante(String nomeSegnalante);

    int getIdAmministratore();
    void setIdAmministratore(int idAmministratore);
}
