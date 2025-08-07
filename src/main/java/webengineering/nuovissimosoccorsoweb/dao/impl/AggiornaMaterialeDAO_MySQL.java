package webengineering.nuovissimosoccorsoweb.dao.impl;

import webengineering.framework.data.DataException;
import webengineering.framework.data.DataLayer;
import webengineering.nuovissimosoccorsoweb.dao.AggiornaMaterialeDAO;
import webengineering.nuovissimosoccorsoweb.model.AggiornaMateriale;
import webengineering.nuovissimosoccorsoweb.model.impl.proxy.AggiornaMaterialeProxy;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import webengineering.framework.data.DAO;

public class AggiornaMaterialeDAO_MySQL extends DAO implements AggiornaMaterialeDAO {

    public AggiornaMaterialeDAO_MySQL(DataLayer dataLayer) {
        super(dataLayer);
    }

    @Override
    public List<AggiornaMateriale> getAggiornamentiByMateriale(int idMateriale) throws DataException {
        List<AggiornaMateriale> list = new ArrayList<>();
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "SELECT * FROM aggiorna_materiale WHERE id_materiale = ?")) {
            stmt.setInt(1, idMateriale);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(makeAggiornamento(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero degli aggiornamenti per materiale", e);
        }
        return list;
    }

    @Override
    public List<AggiornaMateriale> getAggiornamentiByAmministratore(int idAmministratore) throws DataException {
        List<AggiornaMateriale> list = new ArrayList<>();
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "SELECT * FROM aggiorna_materiale WHERE id_am = ?")) {
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
    public void storeAggiornamento(AggiornaMateriale agg) throws DataException {
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "INSERT INTO aggiorna_materiale (id_materiale, nota, id_am) VALUES (?, ?, ?)")) {
            stmt.setInt(1, agg.getIdMateriale());
            stmt.setString(2, agg.getNota());
            stmt.setInt(3, agg.getIdAmministratore());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataException("Errore nell'inserimento dell'aggiornamento materiale", e);
        }
    }

    @Override
    public void deleteAggiornamento(int idMateriale, int idAmministratore, String nota) throws DataException {
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "DELETE FROM aggiorna_materiale WHERE id_materiale = ? AND id_am = ? AND nota = ?")) {
            stmt.setInt(1, idMateriale);
            stmt.setInt(2, idAmministratore);
            stmt.setString(3, nota);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataException("Errore nella cancellazione dell'aggiornamento materiale", e);
        }
    }

    private AggiornaMateriale makeAggiornamento(ResultSet rs) throws SQLException {
        AggiornaMateriale agg = new AggiornaMaterialeProxy();
        agg.setIdMateriale(rs.getInt("id_materiale"));
        agg.setIdAmministratore(rs.getInt("id_am"));
        agg.setNota(rs.getString("nota"));
        agg.setVersion(1); 
        return agg;
    }
}
