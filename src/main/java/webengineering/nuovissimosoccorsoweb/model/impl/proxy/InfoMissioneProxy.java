package webengineering.nuovissimosoccorsoweb.model.impl.proxy;

import webengineering.nuovissimosoccorsoweb.model.impl.InfoMissioneImpl;
import webengineering.framework.data.DataItemProxy;
import webengineering.framework.data.DataLayer;

import java.time.LocalDateTime;

public class InfoMissioneProxy extends InfoMissioneImpl implements DataItemProxy {

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
    public void setSuccesso(int successo) {
        super.setSuccesso(successo);
        this.modified = true;
    }

    @Override
    public void setCommento(String commento) {
        super.setCommento(commento);
        this.modified = true;
    }

    @Override
    public void setDataOraFine(LocalDateTime dataOraFine) {
        super.setDataOraFine(dataOraFine);
        this.modified = true;
    }

    @Override
    public void setVersion(int version) {
        super.setVersion(version);
        this.modified = true;
    }
}
