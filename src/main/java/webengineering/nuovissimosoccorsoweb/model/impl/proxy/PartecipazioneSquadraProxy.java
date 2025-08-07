package webengineering.nuovissimosoccorsoweb.model.impl.proxy;

import webengineering.framework.data.DataItemProxy;
import webengineering.framework.data.DataLayer;
import webengineering.nuovissimosoccorsoweb.model.impl.PartecipazioneSquadraImpl;
import webengineering.nuovissimosoccorsoweb.model.RuoloSquadra;

public class PartecipazioneSquadraProxy extends PartecipazioneSquadraImpl implements DataItemProxy {

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
    public void setIdOperatore(int idOperatore) {
        super.setIdOperatore(idOperatore);
        this.modified = true;
    }

    @Override
    public void setCodiceMissione(int codiceMissione) {
        super.setCodiceMissione(codiceMissione);
        this.modified = true;
    }

    @Override
    public void setRuolo(RuoloSquadra ruolo) {
        super.setRuolo(ruolo);
        this.modified = true;
    }

    @Override
    public void setVersion(int version) {
        super.setVersion(version);
        this.modified = true;
    }
}
