package webengineering.nuovissimosoccorsoweb.dao;

import webengineering.framework.data.DataException;
import webengineering.nuovissimosoccorsoweb.model.TipoPatente;

import java.util.List;

public interface PatenteDAO {

    TipoPatente getPatenteByTipo(String tipo) throws DataException;

    List<TipoPatente> getAllPatenti() throws DataException;

    void storePatente(TipoPatente tipo) throws DataException;

    void deletePatente(TipoPatente tipo) throws DataException;
}
