package webengineering.nuovissimosoccorsoweb.dao.impl;

import webengineering.framework.data.DataException;
import webengineering.framework.data.DataLayer;
import webengineering.nuovissimosoccorsoweb.dao.AbilitaDAO;
import webengineering.nuovissimosoccorsoweb.model.Abilita;
import webengineering.nuovissimosoccorsoweb.model.impl.proxy.AbilitaProxy;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import webengineering.framework.data.DAO;
import webengineering.nuovissimosoccorsoweb.model.TipoAbilita;

public class AbilitaDAO_MySQL extends DAO implements AbilitaDAO {

    public AbilitaDAO_MySQL(DataLayer dataLayer) {
        super(dataLayer);
    }

    @Override
    public Abilita getAbilitaById(int id) throws DataException {
        Abilita abilita = null;

        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "SELECT * FROM abilita WHERE id = ?")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    abilita = makeAbilita(rs);
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero dell'abilità", e);
        }

        return abilita;
    }

    @Override
    public List<Abilita> getAllAbilita() throws DataException {
        List<Abilita> abilita = new ArrayList<>();

        try (Statement stmt = dataLayer.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM abilita")) {
            while (rs.next()) {
                abilita.add(makeAbilita(rs));
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero della lista abilità", e);
        }

        return abilita;
    }

    @Override
    public List<Abilita> getAbilitaByOperatore(int idOperatore) throws DataException {
        List<Abilita> abilita = new ArrayList<>();

        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "SELECT a.* FROM abilita a JOIN ha_abilita ha ON a.id = ha.id_abilita WHERE ha.id_op = ?")) {
            stmt.setInt(1, idOperatore);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    abilita.add(makeAbilita(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero delle abilità dell'operatore", e);
        }

        return abilita;
    }

    @Override
    public void aggiungiAbilitaAOperatore(int idOperatore, int idAbilita) throws DataException {
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "INSERT IGNORE INTO ha_abilita (id_op, id_abilita) VALUES (?, ?)")) {
            stmt.setInt(1, idOperatore);
            stmt.setInt(2, idAbilita);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataException("Errore durante l'aggiunta di un'abilità all'operatore", e);
        }
    }

    @Override
    public void rimuoviAbilitaDaOperatore(int idOperatore, int idAbilita) throws DataException {
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "DELETE FROM ha_abilita WHERE id_op = ? AND id_abilita = ?")) {
            stmt.setInt(1, idOperatore);
            stmt.setInt(2, idAbilita);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataException("Errore durante la rimozione di un'abilità dall'operatore", e);
        }
    }

    @Override
    public void storeAbilita(Abilita abilita) throws DataException {
        if (abilita.getId() > 0) {
            // UPDATE
            try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                    "UPDATE abilita SET tipo = ?, version = ? WHERE id = ?")) {
                stmt.setString(1, abilita.getTipo().name());
                stmt.setInt(2, abilita.getVersion());
                stmt.setInt(3, abilita.getId());
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new DataException("Errore nell'aggiornamento dell'abilità", e);
            }
        } else {
            // INSERT
            try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                    "INSERT INTO abilita (tipo) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, abilita.getTipo().name());
                stmt.executeUpdate();

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        abilita.setId(keys.getInt(1));
                    }
                }
            } catch (SQLException e) {
                throw new DataException("Errore nell'inserimento dell'abilità", e);
            }
        }
    }

    private Abilita makeAbilita(ResultSet rs) throws SQLException {
        Abilita abilita = new AbilitaProxy();
        abilita.setId(rs.getInt("id"));

        // Usa il metodo fromString di TipoAbilita che gestisce spazi -> underscore
        String tipoString = rs.getString("tipo");
        try {
            abilita.setTipo(TipoAbilita.fromString(tipoString));
        } catch (IllegalArgumentException e) {
            // Log dell'errore e usa un valore di default o lancia eccezione più chiara
            throw new SQLException("Tipo abilità non riconosciuto nel database: '" + tipoString + "'. "
                    + "Assicurati che corrisponda a un valore dell'enum TipoAbilita.", e);
        }

        abilita.setVersion(rs.getInt("version"));
        return abilita;
    }
}
