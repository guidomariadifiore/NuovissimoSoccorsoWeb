package webengineering.nuovissimosoccorsoweb.model;

import java.time.LocalDateTime;

public interface InfoMissione {

    int getCodiceMissione(); // chiave e FK verso missione
    void setCodiceMissione(int codice);

    int getSuccesso(); // da 0 a 5
    void setSuccesso(int successo);

    String getCommento();
    void setCommento(String commento);

    LocalDateTime getDataOraFine();
    void setDataOraFine(LocalDateTime dataOraFine);

    int getVersion();
    void setVersion(int version);
}
