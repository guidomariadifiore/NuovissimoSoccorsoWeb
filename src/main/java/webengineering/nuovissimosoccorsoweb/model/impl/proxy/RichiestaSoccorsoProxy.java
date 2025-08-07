package webengineering.nuovissimosoccorsoweb.model.impl.proxy;

import webengineering.framework.data.DataItemProxy;
import webengineering.nuovissimosoccorsoweb.model.impl.RichiestaSoccorsoImpl;

public class RichiestaSoccorsoProxy extends RichiestaSoccorsoImpl implements DataItemProxy {

    private boolean modified;

    @Override
    public boolean isModified() {
        return modified;
    }

    @Override
    public void setModified(boolean modified) {
        this.modified = modified;
    }

    @Override
    public void setCodice(int codice) {
        super.setCodice(codice);
        setModified(true);
    }

    @Override
    public void setStato(String stato) {
        super.setStato(stato);
        setModified(true);
    }

    @Override
    public void setCoordinate(String coordinate) {
        super.setCoordinate(coordinate);
        setModified(true);
    }

    @Override
    public void setIndirizzo(String indirizzo) {
        super.setIndirizzo(indirizzo);
        setModified(true);
    }

    @Override
    public void setDescrizione(String descrizione) {
        super.setDescrizione(descrizione);
        setModified(true);
    }

    @Override
    public void setStringa(String stringa) {
        super.setStringa(stringa);
        setModified(true);
    }

    @Override
    public void setNome(String nome) {
        super.setNome(nome);
        setModified(true);
    }

    @Override
    public void setFoto(String foto) {
        super.setFoto(foto);
        setModified(true);
    }

    @Override
    public void setIp(String ip) {
        super.setIp(ip);
        setModified(true);
    }

    @Override
    public void setEmailSegnalante(String emailSegnalante) {
        super.setEmailSegnalante(emailSegnalante);
        setModified(true);
    }

    @Override
    public void setNomeSegnalante(String nomeSegnalante) {
        super.setNomeSegnalante(nomeSegnalante);
        setModified(true);
    }

    @Override
    public void setIdAmministratore(int idAmministratore) {
        super.setIdAmministratore(idAmministratore);
        setModified(true);
    }
}
