package webengineering.nuovissimosoccorsoweb.dao;

import webengineering.framework.data.DataException;
import webengineering.nuovissimosoccorsoweb.model.Amministratore;

import java.util.List;

public interface AmministratoreDAO {

    Amministratore getAmministratoreById(int id) throws DataException;

    Amministratore getAmministratoreByEmail(String email) throws DataException;

    List<Amministratore> getAllAmministratori() throws DataException;
    
    Amministratore getAmministratoreByCf(String cf) throws DataException;

    void storeAmministratore(Amministratore amministratore) throws DataException;

    void deleteAmministratore(int id) throws DataException;
}
