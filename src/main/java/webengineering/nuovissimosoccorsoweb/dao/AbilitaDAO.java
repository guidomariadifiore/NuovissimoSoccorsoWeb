package webengineering.nuovissimosoccorsoweb.dao;

import webengineering.framework.data.DataException;
import webengineering.nuovissimosoccorsoweb.model.Abilita;

import java.util.List;

public interface AbilitaDAO {

    Abilita getAbilitaById(int id) throws DataException;

    List<Abilita> getAllAbilita() throws DataException;

    List<Abilita> getAbilitaByOperatore(int idOperatore) throws DataException;

    void aggiungiAbilitaAOperatore(int idOperatore, int idAbilita) throws DataException;

    void rimuoviAbilitaDaOperatore(int idOperatore, int idAbilita) throws DataException;

    void storeAbilita(Abilita abilita) throws DataException;
}
