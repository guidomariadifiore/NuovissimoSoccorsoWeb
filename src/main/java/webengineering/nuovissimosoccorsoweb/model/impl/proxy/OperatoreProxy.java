package webengineering.nuovissimosoccorsoweb.model.impl.proxy;

import webengineering.nuovissimosoccorsoweb.model.impl.OperatoreImpl;
import webengineering.framework.data.DataItemProxy;
import webengineering.framework.data.DataLayer;

public class OperatoreProxy extends OperatoreImpl implements DataItemProxy {

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

    // Override setter se vuoi tracciare modifiche in futuro
}
