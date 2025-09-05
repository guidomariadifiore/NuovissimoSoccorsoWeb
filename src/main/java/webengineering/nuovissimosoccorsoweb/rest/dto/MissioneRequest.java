package webengineering.nuovissimosoccorsoweb.rest.dto;

/**
 * DTO per la richiesta di creazione missione.
 */
public class MissioneRequest {
    private int richiestaId;
    private String nome;
    private String posizione;
    private String obiettivo;
    private String operatori;          
    private String caposquadra;        
    private String operatoriStandard;  
    private String mezzi;              
    private String materiali;          
    
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
    
    public String getCaposquadra() { return caposquadra; }
    public void setCaposquadra(String caposquadra) { this.caposquadra = caposquadra; }
    
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