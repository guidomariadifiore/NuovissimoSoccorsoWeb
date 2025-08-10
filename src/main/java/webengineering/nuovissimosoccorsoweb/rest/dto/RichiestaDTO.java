package webengineering.nuovissimosoccorsoweb.rest.dto;

/**
 * DTO per i dati della richiesta di soccorso nelle risposte REST.
 * Contiene tutti i dati della richiesta (incluso il codice generato).
 * 
 * @author YourName
 */
public class RichiestaDTO {
    private int codice;
    private String stato;
    private String indirizzo;
    private String descrizione;
    private String nome;
    private String emailSegnalante;
    private String nomeSegnalante;
    private String coordinate;
    private String foto;
    private String stringaValidazione;
    private Integer idAmministratore;
    
    // Costruttori
    public RichiestaDTO() {}
    
    // Getter e Setter
    public int getCodice() { return codice; }
    public void setCodice(int codice) { this.codice = codice; }
    
    public String getStato() { return stato; }
    public void setStato(String stato) { this.stato = stato; }
    
    public String getIndirizzo() { return indirizzo; }
    public void setIndirizzo(String indirizzo) { this.indirizzo = indirizzo; }
    
    public String getDescrizione() { return descrizione; }
    public void setDescrizione(String descrizione) { this.descrizione = descrizione; }
    
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    
    public String getEmailSegnalante() { return emailSegnalante; }
    public void setEmailSegnalante(String emailSegnalante) { this.emailSegnalante = emailSegnalante; }
    
    public String getNomeSegnalante() { return nomeSegnalante; }
    public void setNomeSegnalante(String nomeSegnalante) { this.nomeSegnalante = nomeSegnalante; }
    
    public String getCoordinate() { return coordinate; }
    public void setCoordinate(String coordinate) { this.coordinate = coordinate; }
    
    public String getFoto() { return foto; }
    public void setFoto(String foto) { this.foto = foto; }
    
    public String getStringaValidazione() { return stringaValidazione; }
    public void setStringaValidazione(String stringaValidazione) { this.stringaValidazione = stringaValidazione; }
    
    public Integer getIdAmministratore() { return idAmministratore; }
    public void setIdAmministratore(Integer idAmministratore) { this.idAmministratore = idAmministratore; }
    
    @Override
    public String toString() {
        return "RichiestaDTO{" +
                "codice=" + codice +
                ", stato='" + stato + '\'' +
                ", indirizzo='" + indirizzo + '\'' +
                ", descrizione='" + descrizione + '\'' +
                ", nome='" + nome + '\'' +
                ", emailSegnalante='" + emailSegnalante + '\'' +
                ", nomeSegnalante='" + nomeSegnalante + '\'' +
                ", coordinate='" + coordinate + '\'' +
                ", foto='" + (foto != null ? "presente" : "null") + '\'' +
                ", stringaValidazione='" + stringaValidazione + '\'' +
                ", idAmministratore=" + idAmministratore +
                '}';
    }
}