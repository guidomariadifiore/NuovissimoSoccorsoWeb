package webengineering.nuovissimosoccorsoweb.rest.dto;

/**
 * DTO per la richiesta di creazione missione.
 * AGGIORNATO per gestire caposquadra e operatori standard separatamente.
 */
public class MissioneRequest {
    private int richiestaId;
    private String nome;
    private String posizione;
    private String obiettivo;
    private String operatori;          // Tutti gli operatori (per compatibilità)
    private String caposquadra;        // NUOVO: IDs caposquadra separati da virgola
    private String operatoriStandard;  // NUOVO: IDs operatori standard separati da virgola
    private String mezzi;              // Targhe separate da virgola (non più ID)
    private String materiali;          // IDs separati da virgola (opzionale)
    
    // Costruttori
    public MissioneRequest() {}
    
    // Getters e Setters
    public int getRichiestaId() { return richiestaId; }
    public void setRichiestaId(int richiestaId) { this.richiestaId = richiestaId; }
    
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    
    public String getPosizione() { return posizione; }
    public void setPosizione(String posizione) { this.posizione = posizione; }
    
    public String getObiettivo() { return obiettivo; }
    public void setObiettivo(String obiettivo) { this.obiettivo = obiettivo; }
    
    public String getOperatori() { return operatori; }
    public void setOperatori(String operatori) { this.operatori = operatori; }
    
    // NUOVI GETTER/SETTER per caposquadra
    public String getCaposquadra() { return caposquadra; }
    public void setCaposquadra(String caposquadra) { this.caposquadra = caposquadra; }
    
    // NUOVI GETTER/SETTER per operatori standard
    public String getOperatoriStandard() { return operatoriStandard; }
    public void setOperatoriStandard(String operatoriStandard) { this.operatoriStandard = operatoriStandard; }
    
    public String getMezzi() { return mezzi; }
    public void setMezzi(String mezzi) { this.mezzi = mezzi; }
    
    public String getMateriali() { return materiali; }
    public void setMateriali(String materiali) { this.materiali = materiali; }
    
    @Override
    public String toString() {
        return "MissioneRequest{" +
                "richiestaId=" + richiestaId +
                ", nome='" + nome + '\'' +
                ", posizione='" + posizione + '\'' +
                ", obiettivo='" + obiettivo + '\'' +
                ", operatori='" + operatori + '\'' +
                ", caposquadra='" + caposquadra + '\'' +
                ", operatoriStandard='" + operatoriStandard + '\'' +
                ", mezzi='" + mezzi + '\'' +
                ", materiali='" + materiali + '\'' +
                '}';
    }
}