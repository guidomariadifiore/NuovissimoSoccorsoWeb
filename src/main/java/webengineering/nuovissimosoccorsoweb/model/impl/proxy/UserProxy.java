package webengineering.nuovissimosoccorsoweb.model.impl.proxy;

import webengineering.framework.data.DataLayer;
import webengineering.nuovissimosoccorsoweb.model.User;
import webengineering.framework.data.DataItemProxy;

import java.util.List;
import webengineering.nuovissimosoccorsoweb.model.Patenti;

public class UserProxy extends webengineering.nuovissimosoccorsoweb.model.impl.UserImpl implements DataItemProxy {

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
    public void setCognome(String cognome) {
        super.setCognome(cognome);
        this.modified = true;
    }

    @Override
    public void setEmail(String email) {
        super.setEmail(email);
        this.modified = true;
    }

    @Override
    public void setPassword(String password) {
        super.setPassword(password);
        this.modified = true;
    }

    @Override
    public void setRuolo(String ruolo) {
        super.setRuolo(ruolo);
        this.modified = true;
    }

    @Override
    public void setPatenti(List<Patenti> patenti) {
        super.setPatenti(patenti);
        this.modified = true;
    }

    @Override
    public void setAbilita(List<String> abilita) {
        super.setAbilita(abilita);
        this.modified = true;
    }

    @Override
    public void setVersion(int version) {
        super.setVersion(version);
        this.modified = true;
    }
} 
