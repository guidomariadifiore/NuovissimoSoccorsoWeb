// ===== MissioneAssociataDTO.java =====
package webengineering.nuovissimosoccorsoweb.rest.dto;

/**
 * DTO per le informazioni di una missione associata a una richiesta.
 */
public class MissioneAssociataDTO {

    private int id;
    private String nome;
    private String posizione;
    private String obiettivo;
    private String stato; // "ATTIVA" o "CONCLUSA"
    private String dataOraInizio;
    private String dataOraFine;
    private int livelloSuccesso; // 0-10, 0 se non valutata
    private String commento;
    private int numeroOperatori;

    // Costruttori
    public MissioneAssociataDTO() {
    }

    // Getters e Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getStato() {
        return stato;
    }

    public void setStato(String stato) {
        this.stato = stato;
    }

    public String getDataOraInizio() {
        return dataOraInizio;
    }

    public void setDataOraInizio(String dataOraInizio) {
        this.dataOraInizio = dataOraInizio;
    }

    public String getDataOraFine() {
        return dataOraFine;
    }

    public void setDataOraFine(String dataOraFine) {
        this.dataOraFine = dataOraFine;
    }

    public int getLivelloSuccesso() {
        return livelloSuccesso;
    }

    public void setLivelloSuccesso(int livelloSuccesso) {
        this.livelloSuccesso = livelloSuccesso;
    }

    public String getCommento() {
        return commento;
    }

    public void setCommento(String commento) {
        this.commento = commento;
    }

    public int getNumeroOperatori() {
        return numeroOperatori;
    }

    public void setNumeroOperatori(int numeroOperatori) {
        this.numeroOperatori = numeroOperatori;
    }
}
