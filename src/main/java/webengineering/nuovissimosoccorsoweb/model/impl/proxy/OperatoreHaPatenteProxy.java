package webengineering.nuovissimosoccorsoweb.model.impl.proxy;

import webengineering.framework.data.DataItemProxy;
import webengineering.framework.data.DataLayer;
import webengineering.nuovissimosoccorsoweb.model.impl.OperatoreHaPatenteImpl;

public class OperatoreHaPatenteProxy extends OperatoreHaPatenteImpl implements DataItemProxy {

    private boolean modified;
    private DataLayer dataLayer;

    @Override
    public boolean isModified() {
        return modified;
    }

    @Override
    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public DataLayer getDataLayer() {
        return dataLayer;
    }

    public void setDataLayer(DataLayer dataLayer) {
        this.dataLayer = dataLayer;
    }

    @Override
    public void setIdOperatore(int idOperatore) {
        super.setIdOperatore(idOperatore);
        this.modified = true;
    }

    @Override
    public void setTipoPatente(String tipoPatente) {
        super.setTipoPatente(tipoPatente);
        this.modified = true;
    }

    @Override
    public void setVersion(int version) {
        super.setVersion(version);
        this.modified = true;
    }
} 
