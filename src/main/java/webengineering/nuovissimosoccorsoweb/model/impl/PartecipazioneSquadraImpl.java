package webengineering.nuovissimosoccorsoweb.model.impl;

import webengineering.nuovissimosoccorsoweb.model.PartecipazioneSquadra;
import webengineering.nuovissimosoccorsoweb.model.RuoloSquadra;

public class PartecipazioneSquadraImpl implements PartecipazioneSquadra {

    private int idOperatore;
    private int codiceMissione;
    private RuoloSquadra ruolo;
    private int version;

    @Override
    public int getIdOperatore() {
        return idOperatore;
    }

    @Override
    public void setIdOperatore(int idOperatore) {
        this.idOperatore = idOperatore;
    }

    @Override
    public int getCodiceMissione() {
        return codiceMissione;
    }

    @Override
    public void setCodiceMissione(int codiceMissione) {
        this.codiceMissione = codiceMissione;
    }

    @Override
    public RuoloSquadra getRuolo() {
        return ruolo;
    }

    @Override
    public void setRuolo(RuoloSquadra ruolo) {
        this.ruolo = ruolo;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public void setVersion(int version) {
        this.version = version;
    }
}
