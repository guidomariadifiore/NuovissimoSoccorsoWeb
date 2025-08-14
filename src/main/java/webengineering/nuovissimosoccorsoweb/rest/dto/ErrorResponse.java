package webengineering.nuovissimosoccorsoweb.rest.dto;

/**
 * DTO per la gestione degli errori nelle API REST.
 * Fornisce una struttura consistente per tutti i tipi di errore.
 */
public class ErrorResponse {
    private String error;
    private String message;
    private String code;
    private Long timestamp;
    
    // Costruttori
    public ErrorResponse() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public ErrorResponse(String error, String code) {
        this();
        this.error = error;
        this.code = code;
    }
    
    public ErrorResponse(String error, String message, String code) {
        this();
        this.error = error;
        this.message = message;
        this.code = code;
    }
    
    // Getters e Setters
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    
    @Override
    public String toString() {
        return "ErrorResponse{" +
                "error='" + error + '\'' +
                ", message='" + message + '\'' +
                ", code='" + code + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}