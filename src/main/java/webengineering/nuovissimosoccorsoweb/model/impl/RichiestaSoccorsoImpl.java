package webengineering.nuovissimosoccorsoweb.model.impl;

import webengineering.nuovissimosoccorsoweb.model.RichiestaSoccorso;

public class RichiestaSoccorsoImpl implements RichiestaSoccorso {

    private int codice;
    private String stato;
    private String coordinate;
    private String indirizzo;
    private String descrizione;
    private String stringa;
    private String nome;
    private String foto;
    private String ip;
    private String emailSegnalante;
    private String nomeSegnalante;
    private int idAmministratore;

    @Override
    public int getCodice() {
        return codice;
    }

    @Override
    public void setCodice(int codice) {
        this.codice = codice;
    }

    @Override
    public String getStato() {
        return stato;
    }

    @Override
    public void setStato(String stato) {
        this.stato = stato;
    }

    @Override
    public String getCoordinate() {
        return coordinate;
    }

    @Override
    public void setCoordinate(String coordinate) {
        this.coordinate = coordinate;
    }

    @Override
    public String getIndirizzo() {
        return indirizzo;
    }

    @Override
    public void setIndirizzo(String indirizzo) {
        this.indirizzo = indirizzo;
    }

    @Override
    public String getDescrizione() {
        return descrizione;
    }

    @Override
    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    @Override
    public String getStringa() {
        return stringa;
    }

    @Override
    public void setStringa(String stringa) {
        this.stringa = stringa;
    }

    @Override
    public String getNome() {
        return nome;
    }

    @Override
    public void setNome(String nome) {
        this.nome = nome;
    }

    @Override
    public String getFoto() {
        return foto;
    }

    @Override
    public void setFoto(String foto) {
        this.foto = foto;
    }

    @Override
    public String getIp() {
        return ip;
    }

    @Override
    public void setIp(String ip) {
        this.ip = ip;
    }

    @Override
    public String getEmailSegnalante() {
        return emailSegnalante;
    }

    @Override
    public void setEmailSegnalante(String emailSegnalante) {
        this.emailSegnalante = emailSegnalante;
    }

    @Override
    public String getNomeSegnalante() {
        return nomeSegnalante;
    }

    @Override
    public void setNomeSegnalante(String nomeSegnalante) {
        this.nomeSegnalante = nomeSegnalante;
    }

    @Override
    public int getIdAmministratore() {
        return idAmministratore;
    }

    @Override
    public void setIdAmministratore(int idAmministratore) {
        this.idAmministratore = idAmministratore;
    }
}
