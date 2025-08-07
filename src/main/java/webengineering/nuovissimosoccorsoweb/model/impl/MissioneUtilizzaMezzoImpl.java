package webengineering.nuovissimosoccorsoweb.model.impl;

import webengineering.nuovissimosoccorsoweb.model.MissioneUtilizzaMezzo;

public class MissioneUtilizzaMezzoImpl implements MissioneUtilizzaMezzo {

    private int codiceMissione;
    private String targaMezzo;
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
    public String getTargaMezzo() {
        return targaMezzo;
    }

    @Override
    public void setTargaMezzo(String targaMezzo) {
        this.targaMezzo = targaMezzo;
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
