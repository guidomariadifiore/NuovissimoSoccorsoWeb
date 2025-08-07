package webengineering.nuovissimosoccorsoweb.model.impl.proxy;

import webengineering.nuovissimosoccorsoweb.model.impl.AmministratoreImpl;
import webengineering.framework.data.DataItemProxy;
import webengineering.framework.data.DataLayer;

public class AmministratoreProxy extends AmministratoreImpl implements DataItemProxy {
    
    private boolean modified;
    private DataLayer dataLayer;
    
    // Costruttore di default (come OperatoreProxy)
    public AmministratoreProxy() {
        super();
        this.modified = false;
    }
    
    // Costruttore con DataLayer (opzionale, per compatibilità)
    public AmministratoreProxy(DataLayer dataLayer) {
        super();
        this.dataLayer = dataLayer;
        this.modified = false;
    }
    
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