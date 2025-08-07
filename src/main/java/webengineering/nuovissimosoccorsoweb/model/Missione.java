package webengineering.nuovissimosoccorsoweb.model;

import java.time.LocalDateTime;

public interface Missione {
    int getCodiceRichiesta(); // chiave primaria e FK verso richiesta_soccorso
    void setCodiceRichiesta(int codiceRichiesta);

    String getNome(); // titolo della missione
    void setNome(String nome);

    String getObiettivo();
    void setObiettivo(String obiettivo);

    String getPosizione();
    void setPosizione(String posizione);

    String getNota(); // campo opzionale
    void setNota(String nota);

    int getIdAmministratore(); // chi ha creato la missione
    void setIdAmministratore(int idAmministratore);

    LocalDateTime getDataOraInizio();
    void setDataOraInizio(LocalDateTime dataOraInizio);

    int getVersion();
    void setVersion(int version);
}
