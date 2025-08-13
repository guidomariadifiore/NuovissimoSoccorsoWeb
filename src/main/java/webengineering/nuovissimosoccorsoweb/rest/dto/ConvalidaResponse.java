package webengineering.nuovissimosoccorsoweb.rest.dto;

/**
 * DTO per la risposta di convalida richieste di soccorso.
 * 
 * @author YourName
 */
public class ConvalidaResponse {
    private boolean success;
    private String message;
    private RichiestaDTO richiesta;
    
    // Costruttori
    public ConvalidaResponse() {}
    
    public ConvalidaResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.richiesta = null;
    }
    
    public ConvalidaResponse(boolean success, String message, RichiestaDTO richiesta) {
        this.success = success;
        this.message = message;
        this.richiesta = richiesta;
    }
    
    // Getter e Setter
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public RichiestaDTO getRichiesta() { return richiesta; }
    public void setRichiesta(RichiestaDTO richiesta) { this.richiesta = richiesta; }
    
    @Override
    public String toString() {
        return "ConvalidaResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", richiesta=" + richiesta +
                '}';
    }
}
