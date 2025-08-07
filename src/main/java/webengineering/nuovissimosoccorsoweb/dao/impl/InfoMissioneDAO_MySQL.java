package webengineering.nuovissimosoccorsoweb.dao.impl;

import webengineering.framework.data.DataException;
import webengineering.framework.data.DataLayer;
import webengineering.nuovissimosoccorsoweb.dao.InfoMissioneDAO;
import webengineering.nuovissimosoccorsoweb.model.InfoMissione;
import webengineering.nuovissimosoccorsoweb.model.impl.proxy.InfoMissioneProxy;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import webengineering.framework.data.DAO;

public class InfoMissioneDAO_MySQL extends DAO implements InfoMissioneDAO {

    public InfoMissioneDAO_MySQL(DataLayer dataLayer) {
        super(dataLayer);
    }

    @Override
    public InfoMissione getInfoByCodiceMissione(int codiceMissione) throws DataException {
        InfoMissione info = null;

        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "SELECT * FROM info_missione WHERE codice_missione = ?")) {
            stmt.setInt(1, codiceMissione);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    info = makeInfo(rs);
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero di info_missione", e);
        }

        return info;
    }
    
    @Override
    public List<InfoMissione> getAllInfoMissioni() throws DataException {
        List<InfoMissione> list = new ArrayList<>();
        try (Statement stmt = dataLayer.getConnection().createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM info_missione ORDER BY data_ora_fine DESC")) {
            while (rs.next()) {
                list.add(makeInfo(rs));
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero di tutte le info_missioni", e);
        }
        return list;
    }

    @Override
    public void storeInfoMissione(InfoMissione info) throws DataException {
        if (getInfoByCodiceMissione(info.getCodiceMissione()) != null) {
            // UPDATE
            try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                    "UPDATE info_missione SET successo = ?, commento = ?, data_ora_fine = ? WHERE codice_missione = ?")) {
                stmt.setInt(1, info.getSuccesso());
                stmt.setString(2, info.getCommento());
                stmt.setTimestamp(3, Timestamp.valueOf(info.getDataOraFine()));
                stmt.setInt(4, info.getCodiceMissione());
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new DataException("Errore nell'aggiornamento di info_missione", e);
            }
        } else {
            // INSERT
            try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                    "INSERT INTO info_missione (codice_missione, successo, commento, data_ora_fine) VALUES (?, ?, ?, ?)")) {
                stmt.setInt(1, info.getCodiceMissione());
                stmt.setInt(2, info.getSuccesso());
                stmt.setString(3, info.getCommento());
                stmt.setTimestamp(4, Timestamp.valueOf(info.getDataOraFine()));
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new DataException("Errore nell'inserimento di info_missione", e);
            }
        }
    }

    @Override
    public void deleteInfoMissione(int codiceMissione) throws DataException {
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "DELETE FROM info_missione WHERE codice_missione = ?")) {
            stmt.setInt(1, codiceMissione);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataException("Errore nella cancellazione di info_missione", e);
        }
    }

    private InfoMissione makeInfo(ResultSet rs) throws SQLException {
        InfoMissione info = new InfoMissioneProxy();
        info.setCodiceMissione(rs.getInt("codice_missione"));
        info.setSuccesso(rs.getInt("successo"));
        info.setCommento(rs.getString("commento"));
        info.setDataOraFine(rs.getTimestamp("data_ora_fine").toLocalDateTime());
        info.setVersion(1); // oppure leggi da colonna se esistente
        return info;
    }
}
