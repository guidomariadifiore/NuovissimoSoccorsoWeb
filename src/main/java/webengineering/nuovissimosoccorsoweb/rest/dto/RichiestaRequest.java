package webengineering.nuovissimosoccorsoweb.rest.dto;

/**
 * DTO per la richiesta di inserimento di una nuova richiesta di soccorso.
 * 
 * @author YourName
 */
public class RichiestaRequest {
    private String descrizione;
    private String indirizzo;
    private String nome;
    private String emailSegnalante;
    private String nomeSegnalante;
    private String coordinate;
    private String foto;
    
    // Costruttori
    public RichiestaRequest() {}
    
    public RichiestaRequest(String descrizione, String indirizzo, String nome, 
                           String emailSegnalante, String nomeSegnalante) {
        this.descrizione = descrizione;
        this.indirizzo = indirizzo;
        this.nome = nome;
        this.emailSegnalante = emailSegnalante;
        this.nomeSegnalante = nomeSegnalante;
    }
    
    // Getter e Setter
    public String getDescrizione() { return descrizione; }
    public void setDescrizione(String descrizione) { this.descrizione = descrizione; }
    
    public String getIndirizzo() { return indirizzo; }
    public void setIndirizzo(String indirizzo) { this.indirizzo = indirizzo; }
    
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
    
    @Override
    public String toString() {
        return "RichiestaRequest{" +
                "descrizione='" + descrizione + '\'' +
                ", indirizzo='" + indirizzo + '\'' +
                ", nome='" + nome + '\'' +
                ", emailSegnalante='" + emailSegnalante + '\'' +
                ", nomeSegnalante='" + nomeSegnalante + '\'' +
                ", coordinate='" + coordinate + '\'' +
                ", foto='" + (foto != null ? "presente" : "null") + '\'' +
                '}';
    }
}