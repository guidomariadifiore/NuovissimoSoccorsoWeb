package webengineering.nuovissimosoccorsoweb.model.impl;

import webengineering.nuovissimosoccorsoweb.model.Patenti;
import webengineering.nuovissimosoccorsoweb.model.TipoPatente;

public class PatentiImpl implements Patenti {

    private TipoPatente tipo;
    private int version;

    @Override
    public TipoPatente getTipo() {
        return tipo;
    }

    @Override
    public void setTipo(TipoPatente tipo) {
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
