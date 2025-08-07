package webengineering.nuovissimosoccorsoweb.dao.impl;

import webengineering.framework.data.DataException;
import webengineering.framework.data.DataLayer;
import webengineering.nuovissimosoccorsoweb.dao.PatenteDAO;
import webengineering.nuovissimosoccorsoweb.model.TipoPatente;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import webengineering.framework.data.DAO;

public class PatenteDAO_MySQL extends DAO implements PatenteDAO {

    public PatenteDAO_MySQL(DataLayer dataLayer) {
        super(dataLayer);
    }

    @Override
    public TipoPatente getPatenteByTipo(String tipo) throws DataException {
        TipoPatente patente = null;
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "SELECT tipo FROM patente WHERE tipo = ?")) {
            stmt.setString(1, tipo);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    patente = TipoPatente.fromString(rs.getString("tipo"));
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero della patente", e);
        }
        return patente;
    }

    @Override
    public List<TipoPatente> getAllPatenti() throws DataException {
        List<TipoPatente> lista = new ArrayList<>();
        try (Statement stmt = dataLayer.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery("SELECT tipo FROM patente")) {
            while (rs.next()) {
                lista.add(TipoPatente.fromString(rs.getString("tipo")));
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero di tutte le patenti", e);
        }
        return lista;
    }

    @Override
    public void storePatente(TipoPatente tipo) throws DataException {
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "INSERT INTO patente (tipo) VALUES (?)")) {
            stmt.setString(1, tipo.toDBString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataException("Errore nell'inserimento della patente", e);
        }
    }

    @Override
    public void deletePatente(TipoPatente tipo) throws DataException {
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "DELETE FROM patente WHERE tipo = ?")) {
            stmt.setString(1, tipo.toDBString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataException("Errore nella cancellazione della patente", e);
        }
    }
}
