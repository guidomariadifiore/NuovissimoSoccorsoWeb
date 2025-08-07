package webengineering.nuovissimosoccorsoweb.dao;

import webengineering.framework.data.DataException;
import webengineering.nuovissimosoccorsoweb.model.Missione;
import webengineering.nuovissimosoccorsoweb.model.Mezzo;
import webengineering.nuovissimosoccorsoweb.model.Materiale;
import webengineering.nuovissimosoccorsoweb.model.PartecipazioneSquadra;

import java.util.List;

public interface MissioneDAO {

    Missione getMissioneByCodice(int codiceRichiesta) throws DataException;

    List<Missione> getAllMissioni() throws DataException;

    List<Missione> getMissioniInCorso() throws DataException;

    List<Missione> getMissioniByOperatore(int idOperatore) throws DataException;

    void storeMissione(Missione missione) throws DataException;

    void deleteMissione(int codiceRichiesta) throws DataException;
    
    void updateNoteMissione(int codiceRichiesta, String nota) throws DataException;

    // ----- Gestione operatori (squadra) -----
    List<PartecipazioneSquadra> getSquadraByMissione(int codiceMissione) throws DataException;

    void assegnaOperatoreAMissione(int idOperatore, int codiceMissione, String ruolo) throws DataException;

    void rimuoviOperatoreDaMissione(int idOperatore, int codiceMissione) throws DataException;

    // ----- Gestione mezzi -----
    List<Mezzo> getMezziByMissione(int codiceMissione) throws DataException;

    void assegnaMezzoAMissione(String targa, int codiceMissione) throws DataException;

    void rimuoviMezzoDaMissione(String targa, int codiceMissione) throws DataException;

    // ----- Gestione materiali -----
    List<Materiale> getMaterialiByMissione(int codiceMissione) throws DataException;

    void assegnaMaterialeAMissione(int idMateriale, int codiceMissione) throws DataException;

    void rimuoviMaterialeDaMissione(int idMateriale, int codiceMissione) throws DataException;
    
    // -------
    
    List<Missione> getMissioniInCorsoByOperatore(int idOperatore) throws DataException;
    
    int countMissioniInCorsoByOperatore(int idOperatore) throws DataException;
    
    int countMissioniCompletateByOperatore(int idOperatore) throws DataException;
}
