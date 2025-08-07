package webengineering.nuovissimosoccorsoweb.dao;

import webengineering.framework.data.DataException;
import webengineering.nuovissimosoccorsoweb.model.Materiale;

import java.util.List;

public interface MaterialeDAO {

    Materiale getMaterialeById(int id) throws DataException;

    List<Materiale> getAllMateriali() throws DataException;

    void storeMateriale(Materiale materiale) throws DataException;

    void deleteMateriale(int id) throws DataException;

    List<Materiale> getMaterialiDisponibili() throws DataException;

    List<Materiale> getMaterialiByMissione(int codiceMissione) throws DataException;
}
