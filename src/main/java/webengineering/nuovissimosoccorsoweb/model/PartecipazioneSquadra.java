package webengineering.nuovissimosoccorsoweb.model;

public interface PartecipazioneSquadra {

    int getIdOperatore();
    void setIdOperatore(int idOperatore);

    int getCodiceMissione();
    void setCodiceMissione(int codiceMissione);

    RuoloSquadra getRuolo();
    void setRuolo(RuoloSquadra ruolo);

    int getVersion();
    void setVersion(int version);
}
