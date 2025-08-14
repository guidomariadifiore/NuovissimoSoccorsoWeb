package webengineering.nuovissimosoccorsoweb.dao;

import webengineering.framework.data.DataException;
import webengineering.nuovissimosoccorsoweb.model.RichiestaSoccorso;

import java.util.List;

public interface RichiestaSoccorsoDAO {

    RichiestaSoccorso getRichiestaByCodice(int codice) throws DataException;

    List<RichiestaSoccorso> getAllRichieste() throws DataException;

    List<RichiestaSoccorso> getRichiesteByStato(String stato) throws DataException;

    List<RichiestaSoccorso> getRichiesteByAmministratore(int idAmministratore) throws DataException;

    List<RichiestaSoccorso> getRichiesteConvalidateNonGestite() throws DataException;

    void updateStato(int codice, String nuovoStato) throws DataException;

    void storeRichiesta(RichiestaSoccorso richiesta) throws DataException;

    void deleteRichiesta(int codice) throws DataException;

    RichiestaSoccorso getRichiestaByStringaValidazione(String stringa) throws DataException;

    /**
     * Recupera richieste con paginazione diretta dal database.
     *
     * @param stato Stato delle richieste (null per tutte)
     * @param offset Offset per la paginazione (0-based)
     * @param limit Numero massimo di risultati
     * @return Lista delle richieste
     */
    List<RichiestaSoccorso> getRichiesteWithPagination(String stato, int offset, int limit) throws DataException;

    /**
     * Conta il numero totale di richieste per stato.
     *
     * @param stato Stato delle richieste (null per tutte)
     * @return Numero totale di richieste
     */
    int countRichiesteByStato(String stato) throws DataException;

    /**
     * Recupera richieste chiuse con livello di successo specifico. Questo
     * metodo è per la specifica "richieste non totalmente positive".
     *
     * @param maxLivelloSuccesso Livello massimo di successo (esclusivo)
     * @param offset Offset per la paginazione
     * @param limit Numero massimo di risultati
     * @return Lista delle richieste
     */
    List<RichiestaSoccorso> getRichiesteChiuseByLivelloSuccesso(int maxLivelloSuccesso, int offset, int limit) throws DataException;

    /**
     * Conta richieste chiuse con livello di successo specifico.
     */
    int countRichiesteChiuseByLivelloSuccesso(int maxLivelloSuccesso) throws DataException;

}
