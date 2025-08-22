package webengineering.nuovissimosoccorsoweb.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * DTO per rappresentare una missione nelle risposte API.
 */
public class MissioneDTO {

    private int codiceRichiesta;
    private String nome;
    private String posizione;
    private String obiettivo;
    private String nota;

    private String dataOraInizio;

    private int idAmministratore;

    // Costruttori
    public MissioneDTO() {
    }

    public MissioneDTO(int codiceRichiesta, String nome, String posizione, String obiettivo) {
        this.codiceRichiesta = codiceRichiesta;
        this.nome = nome;
        this.posizione = posizione;
        this.obiettivo = obiettivo;
    }

    // Getters e Setters
    public int getCodiceRichiesta() {
        return codiceRichiesta;
    }

    public void setCodiceRichiesta(int codiceRichiesta) {
        this.codiceRichiesta = codiceRichiesta;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getPosizione() {
        return posizione;
    }

    public void setPosizione(String posizione) {
        this.posizione = posizione;
    }

    public String getObiettivo() {
        return obiettivo;
    }

    public void setObiettivo(String obiettivo) {
        this.obiettivo = obiettivo;
    }

    public String getNota() {
        return nota;
    }

    public void setNota(String nota) {
        this.nota = nota;
    }

    public String getDataOraInizio() {
        return dataOraInizio;
    }

    public void setDataOraInizio(String dataOraInizio) {
        this.dataOraInizio = dataOraInizio;
    }

    public int getIdAmministratore() {
        return idAmministratore;
    }

    public void setIdAmministratore(int idAmministratore) {
        this.idAmministratore = idAmministratore;
    }

    @Override
    public String toString() {
        return "MissioneDTO{"
                + "codiceRichiesta=" + codiceRichiesta
                + ", nome='" + nome + '\''
                + ", posizione='" + posizione + '\''
                + ", obiettivo='" + obiettivo + '\''
                + ", nota='" + nota + '\''
                + ", dataOraInizio=" + dataOraInizio
                + ", idAmministratore=" + idAmministratore
                + '}';
    }
}
