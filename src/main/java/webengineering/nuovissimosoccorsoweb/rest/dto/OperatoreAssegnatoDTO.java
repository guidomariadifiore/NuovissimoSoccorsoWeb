package webengineering.nuovissimosoccorsoweb.rest.dto;

/**
 * DTO per rappresentare un operatore assegnato a una missione.
 */
public class OperatoreAssegnatoDTO {
    private int id;
    private String nome;
    private String cognome;
    private String email;
    private String codiceFiscale;
    private String ruolo; // "Caposquadra" o "Standard"
    
    // Costruttori
    public OperatoreAssegnatoDTO() {}
    
    public OperatoreAssegnatoDTO(int id, String nome, String cognome, String email, String ruolo) {
        this.id = id;
        this.nome = nome;
        this.cognome = cognome;
        this.email = email;
        this.ruolo = ruolo;
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
    
    public String getRuolo() { return ruolo; }
    public void setRuolo(String ruolo) { this.ruolo = ruolo; }
    
    public String getNomeCompleto() {
        return nome + " " + cognome;
    }
    
    @Override
    public String toString() {
        return "OperatoreAssegnatoDTO{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", cognome='" + cognome + '\'' +
                ", email='" + email + '\'' +
                ", ruolo='" + ruolo + '\'' +
                '}';
    }
}