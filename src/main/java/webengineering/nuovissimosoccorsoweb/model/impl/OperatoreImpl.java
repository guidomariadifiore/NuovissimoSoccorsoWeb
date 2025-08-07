package webengineering.nuovissimosoccorsoweb.model.impl;

import webengineering.nuovissimosoccorsoweb.model.Operatore;

public class OperatoreImpl extends UserImpl implements Operatore {
        private int idAmministratore;
        private String codiceFiscale;



    @Override
    public int getIdAmministratore() {
        return this.idAmministratore;
    }

    @Override
    public void setIdAmministratore(int idAmministratore) {
        this.idAmministratore=idAmministratore;
    }

    @Override
    public String getCodiceFiscale() {
        return this.codiceFiscale;
    }

    @Override
    public void setCodiceFiscale(String codiceFiscale) {
        this.codiceFiscale=codiceFiscale;
    }
    // Per ora eredita tutto da UserImpl
}
