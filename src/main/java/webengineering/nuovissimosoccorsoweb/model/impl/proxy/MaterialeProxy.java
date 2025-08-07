package webengineering.nuovissimosoccorsoweb.model.impl.proxy;

import webengineering.framework.data.DataItemProxy;
import webengineering.framework.data.DataLayer;
import webengineering.nuovissimosoccorsoweb.model.impl.MaterialeImpl;

public class MaterialeProxy extends MaterialeImpl implements DataItemProxy {

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
    public void setNome(String nome) {
        super.setNome(nome);
        this.modified = true;
    }

    @Override
    public void setDescrizione(String descrizione) {
        super.setDescrizione(descrizione);
        this.modified = true;
    }

    @Override
    public void setVersion(int version) {
        super.setVersion(version);
        this.modified = true;
    }
} 
