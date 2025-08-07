package webengineering.nuovissimosoccorsoweb.dao;

import webengineering.framework.data.DataException;
import webengineering.nuovissimosoccorsoweb.model.Abilita;
import webengineering.nuovissimosoccorsoweb.model.Operatore;

import java.util.List;

public interface OperatoreHaAbilitaDAO {

    List<Abilita> getAbilitaByOperatore(int idOperatore) throws DataException;

    void assegnaAbilita(int idOperatore, int idAbilita) throws DataException;

    void rimuoviAbilita(int idOperatore, int idAbilita) throws DataException;

    void rimuoviTutteLeAbilita(int idOperatore) throws DataException;

    List<Operatore> getOperatoriByAbilita(int idAbilita) throws DataException;
}
