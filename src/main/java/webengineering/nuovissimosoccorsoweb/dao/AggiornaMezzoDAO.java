package webengineering.nuovissimosoccorsoweb.dao;

import webengineering.framework.data.DataException;
import webengineering.nuovissimosoccorsoweb.model.AggiornaMezzo;

import java.util.List;

public interface AggiornaMezzoDAO {

    List<AggiornaMezzo> getAggiornamentiByTarga(String targa) throws DataException;

    List<AggiornaMezzo> getAggiornamentiByAmministratore(int idAmministratore) throws DataException;

    void storeAggiornamento(AggiornaMezzo agg) throws DataException;

    void deleteAggiornamento(String targa, int idAmministratore, String nota) throws DataException;
}
