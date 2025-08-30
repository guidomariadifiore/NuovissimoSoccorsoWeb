package webengineering.nuovissimosoccorsoweb.rest.dto;

/**
 * DTO per la valutazione di una missione conclusa.
 */
public class ValutazioneMissioneDTO {

    private int successo; // 1-5 (livello di successo)
    private String commento;

    private String dataOraFine;

    // Costruttori
    public ValutazioneMissioneDTO() {
    }

    public ValutazioneMissioneDTO(int successo, String commento, String dataOraFine) {
        this.successo = successo;
        this.commento = commento;
        this.dataOraFine = dataOraFine;
    }

    // Getters e Setters
    public int getSuccesso() {
        return successo;
    }

    public void setSuccesso(int successo) {
        this.successo = successo;
    }

    public String getCommento() {
        return commento;
    }

    public void setCommento(String commento) {
        this.commento = commento;
    }

    public String getDataOraFine() {
        return dataOraFine;
    }

    public void setDataOraFine(String dataOraFine) {
        this.dataOraFine = dataOraFine;
    }

    /**
     * Restituisce una descrizione testuale del livello di successo.
     */
    public String getSuccessoDescrittivo() {
        switch (successo) {
            case 1:
                return "Molto scarso";
            case 2:
                return "Scarso";
            case 3:
                return "Sufficiente";
            case 4:
                return "Buono";
            case 5:
                return "Eccellente";
            default:
                return "Non valutato";
        }
    }

    @Override
    public String toString() {
        return "ValutazioneMissioneDTO{"
                + "successo=" + successo
                + ", commento='" + commento + '\''
                + ", dataOraFine=" + dataOraFine
                + '}';
    }
}
