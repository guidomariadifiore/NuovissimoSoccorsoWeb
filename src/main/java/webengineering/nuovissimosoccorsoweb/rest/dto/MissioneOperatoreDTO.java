package webengineering.nuovissimosoccorsoweb.rest.dto;

/**
 * DTO per rappresentare una missione dal punto di vista di un operatore.
 */
public class MissioneOperatoreDTO {

    private int id;
    private String nome;
    private String posizione;
    private String obiettivo;
    private String stato; // "ATTIVA" o "CONCLUSA"
    private String dataOraInizio;
    private String dataOraFine;
    private String ruoloOperatore; // "Caposquadra" o "Standard"
    private int livelloSuccesso; // 0-5
    private String descrizioneRichiesta;
    private String indirizzoIntervento;

    // Costruttori
    public MissioneOperatoreDTO() {
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

    public String getRuoloOperatore() {
        return ruoloOperatore;
    }

    public void setRuoloOperatore(String ruoloOperatore) {
        this.ruoloOperatore = ruoloOperatore;
    }

    public int getLivelloSuccesso() {
        return livelloSuccesso;
    }

    public void setLivelloSuccesso(int livelloSuccesso) {
        this.livelloSuccesso = livelloSuccesso;
    }

    public String getDescrizioneRichiesta() {
        return descrizioneRichiesta;
    }

    public void setDescrizioneRichiesta(String descrizioneRichiesta) {
        this.descrizioneRichiesta = descrizioneRichiesta;
    }

    public String getIndirizzoIntervento() {
        return indirizzoIntervento;
    }

    public void setIndirizzoIntervento(String indirizzoIntervento) {
        this.indirizzoIntervento = indirizzoIntervento;
    }
}
