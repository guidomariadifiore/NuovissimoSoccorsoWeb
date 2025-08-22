package webengineering.nuovissimosoccorsoweb.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO per i dettagli completi di una missione.
 * Include operatori, mezzi, materiali e valutazione.
 */
public class DettagliMissioneDTO {
    private int id;
    private String nome;
    private String posizione;
    private String obiettivo;
    private String note;
    private String stato; // "ATTIVA" o "CONCLUSA"
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataOraInizio;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataOraFine;
    
    private int idAmministratore;
    private int richiestaId;
    
    // Risorse associate
    private List<OperatoreAssegnatoDTO> operatori;
    private List<MezzoAssegnatoDTO> mezzi;
    private List<MaterialeAssegnatoDTO> materiali;
    
    // Richiesta e valutazione
    private RichiestaDTO richiesta;
    private ValutazioneMissioneDTO valutazione;
    
    // Costruttori
    public DettagliMissioneDTO() {}
    
    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    
    public String getPosizione() { return posizione; }
    public void setPosizione(String posizione) { this.posizione = posizione; }
    
    public String getObiettivo() { return obiettivo; }
    public void setObiettivo(String obiettivo) { this.obiettivo = obiettivo; }
    
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    
    public String getStato() { return stato; }
    public void setStato(String stato) { this.stato = stato; }
    
    public LocalDateTime getDataOraInizio() { return dataOraInizio; }
    public void setDataOraInizio(LocalDateTime dataOraInizio) { this.dataOraInizio = dataOraInizio; }
    
    public LocalDateTime getDataOraFine() { return dataOraFine; }
    public void setDataOraFine(LocalDateTime dataOraFine) { this.dataOraFine = dataOraFine; }
    
    public int getIdAmministratore() { return idAmministratore; }
    public void setIdAmministratore(int idAmministratore) { this.idAmministratore = idAmministratore; }
    
    public int getRichiestaId() { return richiestaId; }
    public void setRichiestaId(int richiestaId) { this.richiestaId = richiestaId; }
    
    public List<OperatoreAssegnatoDTO> getOperatori() { return operatori; }
    public void setOperatori(List<OperatoreAssegnatoDTO> operatori) { this.operatori = operatori; }
    
    public List<MezzoAssegnatoDTO> getMezzi() { return mezzi; }
    public void setMezzi(List<MezzoAssegnatoDTO> mezzi) { this.mezzi = mezzi; }
    
    public List<MaterialeAssegnatoDTO> getMateriali() { return materiali; }
    public void setMateriali(List<MaterialeAssegnatoDTO> materiali) { this.materiali = materiali; }
    
    public RichiestaDTO getRichiesta() { return richiesta; }
    public void setRichiesta(RichiestaDTO richiesta) { this.richiesta = richiesta; }
    
    public ValutazioneMissioneDTO getValutazione() { return valutazione; }
    public void setValutazione(ValutazioneMissioneDTO valutazione) { this.valutazione = valutazione; }
}