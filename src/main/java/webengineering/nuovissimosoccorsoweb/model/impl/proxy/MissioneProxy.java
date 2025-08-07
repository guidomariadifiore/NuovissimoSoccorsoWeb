package webengineering.nuovissimosoccorsoweb.model.impl.proxy;

import webengineering.nuovissimosoccorsoweb.model.impl.MissioneImpl;
import webengineering.framework.data.DataItemProxy;

public class MissioneProxy extends MissioneImpl implements DataItemProxy {

    private boolean modified;

    @Override
    public boolean isModified() {
        return modified;
    }

    @Override
    public void setModified(boolean modified) {
        this.modified = modified;
    }

    // Override dei metodi setter per attivare il flag "modified"

    @Override
    public void setCodiceRichiesta(int codiceRichiesta) {
        super.setCodiceRichiesta(codiceRichiesta);
        setModified(true);
    }

    @Override
    public void setObiettivo(String obiettivo) {
        super.setObiettivo(obiettivo);
        setModified(true);
    }

    @Override
    public void setPosizione(String posizione) {
        super.setPosizione(posizione);
        setModified(true);
    }

    @Override
    public void setDataOraInizio(java.time.LocalDateTime dataInizio) {
        super.setDataOraInizio(dataInizio);
        setModified(true);
    }

    @Override
    public void setVersion(int version) {
        super.setVersion(version);
        setModified(true);
    }

    @Override
    public void setNome(String nome) {
        super.setNome(nome);
        setModified(true);
    }

    @Override
    public void setNota(String nota) {
        super.setNota(nota);
        setModified(true);
    }

    @Override
    public void setIdAmministratore(int idAmministratore) {
        super.setIdAmministratore(idAmministratore);
        setModified(true);
    }
}
