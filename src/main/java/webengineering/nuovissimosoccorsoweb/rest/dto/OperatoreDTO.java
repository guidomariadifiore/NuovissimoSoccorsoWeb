package webengineering.nuovissimosoccorsoweb.rest.dto;

import java.util.List;

/**
 * DTO per la rappresentazione di un operatore nelle API REST.
 * Non include informazioni sensibili come password.
 * 
 * VERSIONE ESTESA: Include patenti e abilità dell'operatore.
 */
public class OperatoreDTO {
    private int id;
    private String nome;
    private String cognome;
    private String email;
    private String codiceFiscale;
    private boolean disponibile;
    
    // NUOVI CAMPI: Patenti e abilità
    private List<String> patenti;
    private List<String> abilita;
    
    // Informazioni aggiuntive opzionali
    private Integer missioniInCorso;
    private Integer missioniCompletate;
    
    // Costruttori
    public OperatoreDTO() {}
    
    public OperatoreDTO(int id, String nome, String cognome, String email, boolean disponibile) {
        this.id = id;
        this.nome = nome;
        this.cognome = cognome;
        this.email = email;
        this.disponibile = disponibile;
    }
    
    // Getters e Setters esistenti
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    
    public String getCognome() { return cognome; }
    public void setCognome(String cognome) { this.cognome = cognome; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getCodiceFiscale() { return codiceFiscale; }
    public void setCodiceFiscale(String codiceFiscale) { this.codiceFiscale = codiceFiscale; }
    
    public boolean isDisponibile() { return disponibile; }
    public void setDisponibile(boolean disponibile) { this.disponibile = disponibile; }
    
    public Integer getMissioniInCorso() { return missioniInCorso; }
    public void setMissioniInCorso(Integer missioniInCorso) { this.missioniInCorso = missioniInCorso; }
    
    public Integer getMissioniCompletate() { return missioniCompletate; }
    public void setMissioniCompletate(Integer missioniCompletate) { this.missioniCompletate = missioniCompletate; }
    
    // NUOVI Getters e Setters per patenti e abilità
    public List<String> getPatenti() { return patenti; }
    public void setPatenti(List<String> patenti) { this.patenti = patenti; }
    
    public List<String> getAbilita() { return abilita; }
    public void setAbilita(List<String> abilita) { this.abilita = abilita; }
    
    /**
     * Metodo di convenienza per ottenere nome completo.
     */
    public String getNomeCompleto() {
        return nome + " " + cognome;
    }
    
    /**
     * Metodo di convenienza per stato descrittivo.
     */
    public String getStatoDescrittivo() {
        return disponibile ? "Disponibile" : "Impegnato";
    }
    
    /**
     * Verifica se l'operatore ha almeno una patente.
     */
    public boolean hasPatenti() {
        return patenti != null && !patenti.isEmpty();
    }
    
    /**
     * Verifica se l'operatore ha almeno un'abilità.
     */
    public boolean hasAbilita() {
        return abilita != null && !abilita.isEmpty();
    }
    
    @Override
    public String toString() {
        return "OperatoreDTO{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", cognome='" + cognome + '\'' +
                ", email='" + email + '\'' +
                ", codiceFiscale='" + codiceFiscale + '\'' +
                ", disponibile=" + disponibile +
                ", patenti=" + patenti +
                ", abilita=" + abilita +
                ", missioniInCorso=" + missioniInCorso +
                ", missioniCompletate=" + missioniCompletate +
                '}';
    }
}