package webengineering.nuovissimosoccorsoweb.rest.dto;

/**
 * DTO per la risposta di annullamento richiesta.
 */
public class AnnullaRichiestaResponse {
    private boolean success;
    private String message;
    private RichiestaDTO richiesta;
    private String statoOriginale;
    private int adminId;
    
    // Costruttori
    public AnnullaRichiestaResponse() {}
    
    public AnnullaRichiestaResponse(boolean success, String message, RichiestaDTO richiesta, 
                                   String statoOriginale, int adminId) {
        this.success = success;
        this.message = message;
        this.richiesta = richiesta;
        this.statoOriginale = statoOriginale;
        this.adminId = adminId;
    }
    
    // Getters e Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public RichiestaDTO getRichiesta() { return richiesta; }
    public void setRichiesta(RichiestaDTO richiesta) { this.richiesta = richiesta; }
    
    public String getStatoOriginale() { return statoOriginale; }
    public void setStatoOriginale(String statoOriginale) { this.statoOriginale = statoOriginale; }
    
    public int getAdminId() { return adminId; }
    public void setAdminId(int adminId) { this.adminId = adminId; }
}
