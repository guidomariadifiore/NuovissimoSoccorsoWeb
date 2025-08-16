package webengineering.nuovissimosoccorsoweb.rest.dto;

/**
 * DTO per la rappresentazione di un operatore nelle API REST.
 * Non include informazioni sensibili come password.
 */
public class OperatoreDTO {
    private int id;
    private String nome;
    private String cognome;
    private String email;
    private String codiceFiscale;
    private boolean disponibile;
    
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
    
    // Getters e Setters
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
    
    @Override
    public String toString() {
        return "OperatoreDTO{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", cognome='" + cognome + '\'' +
                ", email='" + email + '\'' +
                ", disponibile=" + disponibile +
                ", missioniInCorso=" + missioniInCorso +
                ", missioniCompletate=" + missioniCompletate +
                '}';
    }
}