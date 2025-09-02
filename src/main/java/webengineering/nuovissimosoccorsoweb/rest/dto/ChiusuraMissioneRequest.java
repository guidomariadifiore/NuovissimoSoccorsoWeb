package webengineering.nuovissimosoccorsoweb.rest.dto;

/**
 * DTO per la richiesta di chiusura missione.
 */
public class ChiusuraMissioneRequest {
    private int livelloSuccesso; // 1-5
    private String commento;
    
    // Costruttori
    public ChiusuraMissioneRequest() {}
    
    public ChiusuraMissioneRequest(int livelloSuccesso, String commento) {
        this.livelloSuccesso = livelloSuccesso;
        this.commento = commento;
    }
    
    // Getters e Setters
    public int getLivelloSuccesso() { return livelloSuccesso; }
    public void setLivelloSuccesso(int livelloSuccesso) { this.livelloSuccesso = livelloSuccesso; }
    
    public String getCommento() { return commento; }
    public void setCommento(String commento) { this.commento = commento; }
    
    // Validazione
    public boolean isValid() {
        return livelloSuccesso >= 1 && livelloSuccesso <= 5 
               && commento != null && !commento.trim().isEmpty();
    }
    
    @Override
    public String toString() {
        return "ChiusuraMissioneRequest{" +
                "livelloSuccesso=" + livelloSuccesso +
                ", commento='" + commento + '\'' +
                '}';
    }
}

