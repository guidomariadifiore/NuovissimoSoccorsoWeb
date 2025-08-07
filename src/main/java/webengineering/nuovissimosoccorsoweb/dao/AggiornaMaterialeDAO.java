package webengineering.nuovissimosoccorsoweb.dao;

import webengineering.framework.data.DataException;
import webengineering.nuovissimosoccorsoweb.model.AggiornaMateriale;

import java.util.List;

public interface AggiornaMaterialeDAO {

    List<AggiornaMateriale> getAggiornamentiByMateriale(int idMateriale) throws DataException;

    List<AggiornaMateriale> getAggiornamentiByAmministratore(int idAmministratore) throws DataException;

    void storeAggiornamento(AggiornaMateriale agg) throws DataException;

    void deleteAggiornamento(int idMateriale, int idAmministratore, String nota) throws DataException;
}
