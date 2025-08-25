package webengineering.nuovissimosoccorsoweb.rest.dto;

import java.util.List;

/**
 * Risposta per la lista delle missioni di un operatore.
 */
public class MissioniOperatoreResponse {

    private int operatoreId;
    private String nomeOperatore;
    private String emailOperatore;
    private List<MissioneOperatoreDTO> missioni;
    private int totaleMissioni;

    // Costruttori
    public MissioniOperatoreResponse() {
    }

    public MissioniOperatoreResponse(int operatoreId, String nomeOperatore, String emailOperatore,
            List<MissioneOperatoreDTO> missioni, int totaleMissioni) {
        this.operatoreId = operatoreId;
        this.nomeOperatore = nomeOperatore;
        this.emailOperatore = emailOperatore;
        this.missioni = missioni;
        this.totaleMissioni = totaleMissioni;
    }

    // Getters e Setters
    public int getOperatoreId() {
        return operatoreId;
    }

    public void setOperatoreId(int operatoreId) {
        this.operatoreId = operatoreId;
    }

    public String getNomeOperatore() {
        return nomeOperatore;
    }

    public void setNomeOperatore(String nomeOperatore) {
        this.nomeOperatore = nomeOperatore;
    }

    public String getEmailOperatore() {
        return emailOperatore;
    }

    public void setEmailOperatore(String emailOperatore) {
        this.emailOperatore = emailOperatore;
    }

    public List<MissioneOperatoreDTO> getMissioni() {
        return missioni;
    }

    public void setMissioni(List<MissioneOperatoreDTO> missioni) {
        this.missioni = missioni;
    }

    public int getTotaleMissioni() {
        return totaleMissioni;
    }

    public void setTotaleMissioni(int totaleMissioni) {
        this.totaleMissioni = totaleMissioni;
    }
}
