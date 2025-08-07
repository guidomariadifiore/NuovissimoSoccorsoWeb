package webengineering.nuovissimosoccorsoweb.model.impl.proxy;

import webengineering.framework.data.DataItemProxy;
import webengineering.framework.data.DataLayer;
import webengineering.nuovissimosoccorsoweb.model.impl.AggiornaMezzoImpl;

public class AggiornaMezzoProxy extends AggiornaMezzoImpl implements DataItemProxy {

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
    public void setTarga(String targa) {
        super.setTarga(targa);
        this.modified = true;
    }

    @Override
    public void setIdAmministratore(int idAmministratore) {
        super.setIdAmministratore(idAmministratore);
        this.modified = true;
    }

    @Override
    public void setNota(String nota) {
        super.setNota(nota);
        this.modified = true;
    }

    @Override
    public void setVersion(int version) {
        super.setVersion(version);
        this.modified = true;
    }
} 
