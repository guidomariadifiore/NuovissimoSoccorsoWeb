package webengineering.nuovissimosoccorsoweb.dao.impl;

import webengineering.framework.data.DataException;
import webengineering.framework.data.DataLayer;
import webengineering.nuovissimosoccorsoweb.dao.OperatoreHaPatenteDAO;
import webengineering.nuovissimosoccorsoweb.model.Operatore;
import webengineering.nuovissimosoccorsoweb.model.TipoPatente;
import webengineering.nuovissimosoccorsoweb.model.impl.proxy.OperatoreProxy;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import webengineering.framework.data.DAO;

public class OperatoreHaPatenteDAO_MySQL extends DAO implements OperatoreHaPatenteDAO {

    public OperatoreHaPatenteDAO_MySQL(DataLayer dataLayer) {
        super(dataLayer);
    }

    @Override
    public List<TipoPatente> getPatentiByOperatore(int idOperatore) throws DataException {
        List<TipoPatente> lista = new ArrayList<>();
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "SELECT tipo FROM ha_patente WHERE id_op = ?")) {
            stmt.setInt(1, idOperatore);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(TipoPatente.fromString(rs.getString("tipo")));
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero delle patenti dell'operatore", e);
        }
        return lista;
    }

    @Override
    public void assegnaPatente(int idOperatore, TipoPatente tipo) throws DataException {
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "INSERT INTO ha_patente (id_op, tipo) VALUES (?, ?)")) {
            stmt.setInt(1, idOperatore);
            stmt.setString(2, tipo.toDBString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataException("Errore nell'assegnazione della patente all'operatore", e);
        }
    }

    @Override
    public void rimuoviPatente(int idOperatore, TipoPatente tipo) throws DataException {
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "DELETE FROM ha_patente WHERE id_op = ? AND tipo = ?")) {
            stmt.setInt(1, idOperatore);
            stmt.setString(2, tipo.toDBString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataException("Errore nella rimozione della patente all'operatore", e);
        }
    }

    @Override
    public void rimuoviTutteLePatenti(int idOperatore) throws DataException {
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "DELETE FROM ha_patente WHERE id_op = ?")) {
            stmt.setInt(1, idOperatore);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataException("Errore nella rimozione di tutte le patenti dell'operatore", e);
        }
    }

    @Override
    public List<Operatore> getOperatoriByPatente(TipoPatente tipo) throws DataException {
        List<Operatore> list = new ArrayList<>();
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "SELECT o.* FROM operatore o " +
                "JOIN ha_patente hp ON o.id = hp.id_op " +
                "WHERE hp.tipo = ?")) {
            stmt.setString(1, tipo.toDBString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Operatore op = new OperatoreProxy();
                    op.setId(rs.getInt("id"));
                    op.setNome(rs.getString("nome"));
                    op.setCognome(rs.getString("cognome"));
                    op.setEmail(rs.getString("email"));
                    op.setPassword(rs.getString("password"));
                    op.setCodiceFiscale(rs.getString("cf"));
                    op.setIdAmministratore(rs.getInt("id_am"));
                    op.setVersion(rs.getInt("version"));
                    list.add(op);
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero degli operatori con una certa patente", e);
        }
        return list;
    }
}
