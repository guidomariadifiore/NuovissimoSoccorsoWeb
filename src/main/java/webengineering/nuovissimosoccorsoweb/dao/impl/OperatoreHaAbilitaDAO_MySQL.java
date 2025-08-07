package webengineering.nuovissimosoccorsoweb.dao.impl;

import webengineering.framework.data.DataException;
import webengineering.framework.data.DataLayer;
import webengineering.nuovissimosoccorsoweb.dao.OperatoreHaAbilitaDAO;
import webengineering.nuovissimosoccorsoweb.model.Abilita;
import webengineering.nuovissimosoccorsoweb.model.Operatore;
import webengineering.nuovissimosoccorsoweb.model.impl.proxy.AbilitaProxy;
import webengineering.nuovissimosoccorsoweb.model.impl.proxy.OperatoreProxy;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import webengineering.framework.data.DAO;
import webengineering.nuovissimosoccorsoweb.model.TipoAbilita;

public class OperatoreHaAbilitaDAO_MySQL extends DAO implements OperatoreHaAbilitaDAO {


    public OperatoreHaAbilitaDAO_MySQL(DataLayer dataLayer) {
        super(dataLayer);
    }

    @Override
    public List<Abilita> getAbilitaByOperatore(int idOperatore) throws DataException {
        List<Abilita> abilita = new ArrayList<>();
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "SELECT a.* FROM abilita a JOIN ha_abilita ha ON a.id = ha.id_abilita WHERE ha.id_op = ?")) {
            stmt.setInt(1, idOperatore);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Abilita ab = new AbilitaProxy();
                    ab.setId(rs.getInt("id"));
                    ab.setTipo(TipoAbilita.fromString(rs.getString("tipo")));
                    abilita.add(ab);
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero delle abilità dell'operatore", e);
        }
        return abilita;
    }

    @Override
    public void assegnaAbilita(int idOperatore, int idAbilita) throws DataException {
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "INSERT INTO ha_abilita (id_op, id_abilita) VALUES (?, ?)")) {
            stmt.setInt(1, idOperatore);
            stmt.setInt(2, idAbilita);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataException("Errore nell'assegnazione dell'abilità all'operatore", e);
        }
    }

    @Override
    public void rimuoviAbilita(int idOperatore, int idAbilita) throws DataException {
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "DELETE FROM ha_abilita WHERE id_op = ? AND id_abilita = ?")) {
            stmt.setInt(1, idOperatore);
            stmt.setInt(2, idAbilita);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataException("Errore nella rimozione dell'abilità all'operatore", e);
        }
    }

    @Override
    public void rimuoviTutteLeAbilita(int idOperatore) throws DataException {
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "DELETE FROM ha_abilita WHERE id_op = ?")) {
            stmt.setInt(1, idOperatore);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataException("Errore nella rimozione di tutte le abilità dell'operatore", e);
        }
    }

    @Override
    public List<Operatore> getOperatoriByAbilita(int idAbilita) throws DataException {
        List<Operatore> list = new ArrayList<>();
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "SELECT o.* FROM operatore o JOIN ha_abilita ha ON o.id = ha.id_op WHERE ha.id_abilita = ?")) {
            stmt.setInt(1, idAbilita);
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
            throw new DataException("Errore nel recupero degli operatori con una certa abilità", e);
        }
        return list;
    }
}
