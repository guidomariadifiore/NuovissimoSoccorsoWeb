package webengineering.nuovissimosoccorsoweb.dao;

import webengineering.framework.data.DataException;
import webengineering.nuovissimosoccorsoweb.model.Mezzo;

import java.util.List;

public interface MezzoDAO {

    Mezzo getMezzoByTarga(String targa) throws DataException;

    List<Mezzo> getAllMezzi() throws DataException;

    List<Mezzo> getMezziDisponibili() throws DataException;

    List<Mezzo> getMezziByMissione(int codiceMissione) throws DataException;

    void storeMezzo(Mezzo mezzo) throws DataException;

    void deleteMezzo(String targa) throws DataException;
}
