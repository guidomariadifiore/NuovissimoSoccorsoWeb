package webengineering.nuovissimosoccorsoweb.dao;

import webengineering.framework.data.DataException;
import webengineering.nuovissimosoccorsoweb.model.TipoPatente;

import java.util.List;
import webengineering.nuovissimosoccorsoweb.model.Operatore;

public interface OperatoreHaPatenteDAO {

    List<TipoPatente> getPatentiByOperatore(int idOperatore) throws DataException;
    
    List<Operatore> getOperatoriByPatente(TipoPatente tipo) throws DataException;

    void assegnaPatente(int idOperatore, TipoPatente tipo) throws DataException;

    void rimuoviPatente(int idOperatore, TipoPatente tipo) throws DataException;

    void rimuoviTutteLePatenti(int idOperatore) throws DataException;
}
