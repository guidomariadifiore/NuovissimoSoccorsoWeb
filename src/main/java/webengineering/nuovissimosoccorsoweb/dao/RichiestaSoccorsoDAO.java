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
}
