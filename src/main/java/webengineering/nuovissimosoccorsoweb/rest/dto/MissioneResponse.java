package webengineering.nuovissimosoccorsoweb.rest.dto;

import java.util.List;

/**
 * DTO per la risposta di creazione missione.
 */
public class MissioneResponse {
    private boolean success;
    private String message;
    private MissioneDTO missione;
    private List<String> emailOperatori;
    
    // Costruttori
    public MissioneResponse() {}
    
    public MissioneResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public MissioneResponse(boolean success, String message, MissioneDTO missione, List<String> emailOperatori) {
        this.success = success;
        this.message = message;
        this.missione = missione;
        this.emailOperatori = emailOperatori;
    }
    
    // Getters e Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public MissioneDTO getMissione() { return missione; }
    public void setMissione(MissioneDTO missione) { this.missione = missione; }
    
    public List<String> getEmailOperatori() { return emailOperatori; }
    public void setEmailOperatori(List<String> emailOperatori) { this.emailOperatori = emailOperatori; }
    
    @Override
    public String toString() {
        return "MissioneResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", missione=" + missione +
                ", emailOperatori=" + emailOperatori +
                '}';
    }
}