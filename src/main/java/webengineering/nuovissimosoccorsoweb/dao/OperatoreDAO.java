package webengineering.nuovissimosoccorsoweb.dao;

import webengineering.framework.data.DataException;
import webengineering.nuovissimosoccorsoweb.model.Operatore;

import java.util.List;

public interface OperatoreDAO {

    Operatore getOperatoreById(int id) throws DataException;

    Operatore getOperatoreByEmail(String email) throws DataException;

    List<Operatore> getAllOperatori() throws DataException;
    
    Operatore getOperatoreByCf (String cf) throws DataException;

    List<Operatore> getOperatoriDisponibili() throws DataException;

    void storeOperatore(Operatore operatore) throws DataException;

    void deleteOperatore(int id) throws DataException;
}
