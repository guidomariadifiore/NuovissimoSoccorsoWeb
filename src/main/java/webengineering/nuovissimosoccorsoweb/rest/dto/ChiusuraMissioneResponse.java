package webengineering.nuovissimosoccorsoweb.rest.dto;
/**
 * DTO per la risposta di chiusura missione.
 */
public class ChiusuraMissioneResponse {
    private boolean success;
    private String message;
    private String timestampChiusura;
    private int missioneId;
    
    // Costruttori
    public ChiusuraMissioneResponse() {}
    
    public ChiusuraMissioneResponse(boolean success, String message, String timestampChiusura, int missioneId) {
        this.success = success;
        this.message = message;
        this.timestampChiusura = timestampChiusura;
        this.missioneId = missioneId;
    }
    
    // Factory methods
    public static ChiusuraMissioneResponse success(String message, String timestamp, int missioneId) {
        return new ChiusuraMissioneResponse(true, message, timestamp, missioneId);
    }
    
    public static ChiusuraMissioneResponse error(String message) {
        return new ChiusuraMissioneResponse(false, message, null, 0);
    }
    
    // Getters e Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getTimestampChiusura() { return timestampChiusura; }
    public void setTimestampChiusura(String timestampChiusura) { this.timestampChiusura = timestampChiusura; }
    
    public int getMissioneId() { return missioneId; }
    public void setMissioneId(int missioneId) { this.missioneId = missioneId; }
}