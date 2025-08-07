package webengineering.nuovissimosoccorsoweb.model.impl;

import webengineering.nuovissimosoccorsoweb.model.AggiornaMateriale;

public class AggiornaMaterialeImpl implements AggiornaMateriale {

    private int idMateriale;
    private int idAmministratore;
    private String nota;
    private int version;

    @Override
    public int getIdMateriale() {
        return idMateriale;
    }

    @Override
    public void setIdMateriale(int idMateriale) {
        this.idMateriale = idMateriale;
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
