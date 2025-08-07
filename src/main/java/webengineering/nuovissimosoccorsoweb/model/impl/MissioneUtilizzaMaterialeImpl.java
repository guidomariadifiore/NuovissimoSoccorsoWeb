package webengineering.nuovissimosoccorsoweb.model.impl;

import webengineering.nuovissimosoccorsoweb.model.MissioneUtilizzaMateriale;

public class MissioneUtilizzaMaterialeImpl implements MissioneUtilizzaMateriale {

    private int codiceMissione;
    private int idMateriale;
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
    public int getIdMateriale() {
        return idMateriale;
    }

    @Override
    public void setIdMateriale(int idMateriale) {
        this.idMateriale = idMateriale;
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
