package webengineering.nuovissimosoccorsoweb.model.impl;

import webengineering.nuovissimosoccorsoweb.model.Missione;

import java.time.LocalDateTime;

public class MissioneImpl implements Missione {

    private int codiceRichiesta;
    private String obiettivo;
    private String posizione;
    private LocalDateTime dataInizio;
    private int version;
    private String nome;
    private String nota;
    private int idAmministratore;

    @Override
    public int getCodiceRichiesta() {
        return codiceRichiesta;
    }

    @Override
    public void setCodiceRichiesta(int codiceRichiesta) {
        this.codiceRichiesta = codiceRichiesta;
    }

    @Override
    public String getObiettivo() {
        return obiettivo;
    }

    @Override
    public void setObiettivo(String obiettivo) {
        this.obiettivo = obiettivo;
    }

    @Override
    public String getPosizione() {
        return posizione;
    }

    @Override
    public void setPosizione(String posizione) {
        this.posizione = posizione;
    }

    @Override
    public LocalDateTime getDataOraInizio() {
        return dataInizio;
    }

    @Override
    public void setDataOraInizio(LocalDateTime dataInizio) {
        this.dataInizio = dataInizio;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String getNome() {
        return this.nome;
    }

    @Override
    public void setNome(String nome) {
        this.nome=nome;
    }

    @Override
    public String getNota() {
        return this.nota;
    }

    @Override
    public void setNota(String nota) {
        this.nota=nota;
    }

    @Override
    public int getIdAmministratore() {
        return this.idAmministratore;
    }

    @Override
    public void setIdAmministratore(int idAmministratore) {
        this.idAmministratore=idAmministratore;
    }
} 
