package webengineering.nuovissimosoccorsoweb.model.impl;

import webengineering.nuovissimosoccorsoweb.model.Abilita;
import webengineering.nuovissimosoccorsoweb.model.TipoAbilita;

public class AbilitaImpl implements Abilita {

    private int id;
    private TipoAbilita tipo;
    private int version;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public TipoAbilita getTipo() {
        return tipo;
    }

    @Override
    public void setTipo(TipoAbilita tipo) {
        this.tipo = tipo;
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
