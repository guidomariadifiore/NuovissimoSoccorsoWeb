package webengineering.nuovissimosoccorsoweb.model.impl;

import webengineering.nuovissimosoccorsoweb.model.InfoMissione;

import java.time.LocalDateTime;

public class InfoMissioneImpl implements InfoMissione {

    private int codiceMissione;
    private int successo;
    private String commento;
    private LocalDateTime dataOraFine;
    private int version;

    @Override
    public int getCodiceMissione() {
        return codiceMissione;
    }

    @Override
    public void setCodiceMissione(int codiceMissione) {
        this.codiceMissione = codiceMissione;
    }

    @Override
    public int getSuccesso() {
        return successo;
    }

    @Override
    public void setSuccesso(int successo) {
        this.successo = successo;
    }

    @Override
    public String getCommento() {
        return commento;
    }

    @Override
    public void setCommento(String commento) {
        this.commento = commento;
    }

    @Override
    public LocalDateTime getDataOraFine() {
        return dataOraFine;
    }

    @Override
    public void setDataOraFine(LocalDateTime dataOraFine) {
        this.dataOraFine = dataOraFine;
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
