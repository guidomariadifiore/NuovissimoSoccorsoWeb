package webengineering.nuovissimosoccorsoweb.dao;

import java.util.List;
import webengineering.framework.data.DataException;
import webengineering.nuovissimosoccorsoweb.model.InfoMissione;

public interface InfoMissioneDAO {

    InfoMissione getInfoByCodiceMissione(int codiceMissione) throws DataException;
    
    List<InfoMissione> getAllInfoMissioni() throws DataException;

    void storeInfoMissione(InfoMissione info) throws DataException;

    void deleteInfoMissione(int codiceMissione) throws DataException;
}
