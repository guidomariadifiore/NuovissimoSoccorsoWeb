package webengineering.nuovissimosoccorsoweb.model.impl;

import webengineering.nuovissimosoccorsoweb.model.OperatoreHaPatente;

public class OperatoreHaPatenteImpl implements OperatoreHaPatente {

    private int idOperatore;
    private String tipoPatente;
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
    public String getTipoPatente() {
        return tipoPatente;
    }

    @Override
    public void setTipoPatente(String tipoPatente) {
        this.tipoPatente = tipoPatente;
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
