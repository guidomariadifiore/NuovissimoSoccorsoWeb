package webengineering.nuovissimosoccorsoweb.dao.impl;

import webengineering.framework.data.DataException;
import webengineering.framework.data.DataLayer;
import webengineering.nuovissimosoccorsoweb.dao.AggiornaMezzoDAO;
import webengineering.nuovissimosoccorsoweb.model.AggiornaMezzo;
import webengineering.nuovissimosoccorsoweb.model.impl.proxy.AggiornaMezzoProxy;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import webengineering.framework.data.DAO;

public class AggiornaMezzoDAO_MySQL extends DAO implements AggiornaMezzoDAO {

    public AggiornaMezzoDAO_MySQL(DataLayer dataLayer) {
        super(dataLayer);
    }

    @Override
    public List<AggiornaMezzo> getAggiornamentiByTarga(String targa) throws DataException {
        List<AggiornaMezzo> list = new ArrayList<>();
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "SELECT * FROM aggiorna_mezzo WHERE targa = ?")) {
            stmt.setString(1, targa);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(makeAggiornamento(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero degli aggiornamenti per mezzo", e);
        }
        return list;
    }

    @Override
    public List<AggiornaMezzo> getAggiornamentiByAmministratore(int idAmministratore) throws DataException {
        List<AggiornaMezzo> list = new ArrayList<>();
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "SELECT * FROM aggiorna_mezzo WHERE id_am = ?")) {
            stmt.setInt(1, idAmministratore);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(makeAggiornamento(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero degli aggiornamenti per amministratore", e);
        }
        return list;
    }

    @Override
    public void storeAggiornamento(AggiornaMezzo agg) throws DataException {
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "INSERT INTO aggiorna_mezzo (targa, nota, id_am) VALUES (?, ?, ?)")) {
            stmt.setString(1, agg.getTarga());
            stmt.setString(2, agg.getNota());
            stmt.setInt(3, agg.getIdAmministratore());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataException("Errore nell'inserimento dell'aggiornamento mezzo", e);
        }
    }

    @Override
    public void deleteAggiornamento(String targa, int idAmministratore, String nota) throws DataException {
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "DELETE FROM aggiorna_mezzo WHERE targa = ? AND id_am = ? AND nota = ?")) {
            stmt.setString(1, targa);
            stmt.setInt(2, idAmministratore);
            stmt.setString(3, nota);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataException("Errore nella cancellazione dell'aggiornamento mezzo", e);
        }
    }

    private AggiornaMezzo makeAggiornamento(ResultSet rs) throws SQLException {
        AggiornaMezzo agg = new AggiornaMezzoProxy();
        agg.setTarga(rs.getString("targa"));
        agg.setIdAmministratore(rs.getInt("id_am"));
        agg.setNota(rs.getString("nota"));
        agg.setVersion(1); 
        return agg;
    }
}
