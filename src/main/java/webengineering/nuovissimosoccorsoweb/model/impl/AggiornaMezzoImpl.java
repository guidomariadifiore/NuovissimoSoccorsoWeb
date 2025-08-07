package webengineering.nuovissimosoccorsoweb.model.impl;

import webengineering.nuovissimosoccorsoweb.model.AggiornaMezzo;

public class AggiornaMezzoImpl implements AggiornaMezzo {

    private String targa;
    private int idAmministratore;
    private String nota;
    private int version;

    @Override
    public String getTarga() {
        return targa;
    }

    @Override
    public void setTarga(String targa) {
        this.targa = targa;
    }

    @Override
    public int getIdAmministratore() {
        return idAmministratore;
    }

    @Override
    public void setIdAmministratore(int idAmministratore) {
        this.idAmministratore = idAmministratore;
    }

    @Override
    public String getNota() {
        return nota;
    }

    @Override
    public void setNota(String nota) {
        this.nota = nota;
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
