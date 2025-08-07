package webengineering.nuovissimosoccorsoweb.model.impl.proxy;

import webengineering.framework.data.DataItemProxy;
import webengineering.framework.data.DataLayer;
import webengineering.nuovissimosoccorsoweb.model.impl.MissioneUtilizzaMaterialeImpl;

public class MissioneUtilizzaMaterialeProxy extends MissioneUtilizzaMaterialeImpl implements DataItemProxy {

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

    public void setDataLayer(DataLayer dataLayer) {
        this.dataLayer = dataLayer;
    }

    public DataLayer getDataLayer() {
        return dataLayer;
    }

    @Override
    public void setCodiceMissione(int codiceMissione) {
        super.setCodiceMissione(codiceMissione);
        this.modified = true;
    }

    @Override
    public void setIdMateriale(int idMateriale) {
        super.setIdMateriale(idMateriale);
        this.modified = true;
    }

    @Override
    public void setVersion(int version) {
        super.setVersion(version);
        this.modified = true;
    }
}
