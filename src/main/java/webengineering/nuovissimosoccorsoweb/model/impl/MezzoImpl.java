package webengineering.nuovissimosoccorsoweb.model.impl;

import webengineering.nuovissimosoccorsoweb.model.Mezzo;

public class MezzoImpl implements Mezzo {

    private String targa;
    private String nome;
    private String descrizione;
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
    public String getNome() {
        return nome;
    }

    @Override
    public void setNome(String nome) {
        this.nome = nome;
    }

    @Override
    public String getDescrizione() {
        return descrizione;
    }

    @Override
    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
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
