package webengineering.nuovissimosoccorsoweb.dao.impl;

import webengineering.framework.data.DataException;
import webengineering.framework.data.DataLayer;
import webengineering.framework.data.DAO;
import webengineering.nuovissimosoccorsoweb.dao.AmministratoreDAO;
import webengineering.nuovissimosoccorsoweb.model.Amministratore;
import webengineering.nuovissimosoccorsoweb.model.impl.proxy.AmministratoreProxy;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AmministratoreDAO_MySQL extends DAO implements AmministratoreDAO {

    public AmministratoreDAO_MySQL(DataLayer dataLayer) {
        super(dataLayer);
    }

    @Override
    public Amministratore getAmministratoreById(int id) throws DataException {
        Amministratore a = null;

        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM amministratore WHERE id = ?")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    a = makeAmministratore(rs);
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero dell'amministratore per ID", e);
        }

        return a;
    }

    @Override
    public Amministratore getAmministratoreByEmail(String email) throws DataException {
        Amministratore a = null;

        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM amministratore WHERE email = ?")) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    a = makeAmministratore(rs);
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero dell'amministratore per email", e);
        }

        return a;
    }

    @Override
    public List<Amministratore> getAllAmministratori() throws DataException {
        List<Amministratore> result = new ArrayList<>();

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM amministratore")) {
            while (rs.next()) {
                result.add(makeAmministratore(rs));
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero della lista degli amministratori", e);
        }

        return result;
    }
    
    @Override
    public Amministratore getAmministratoreByCf(String cf) throws DataException {
        Amministratore a = null;

        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM amministratore WHERE cf = ?")) {
            stmt.setString(1, cf);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    a = makeAmministratore(rs);
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero dell'amministratore per codice fiscale", e);
        }

        return a;
    }

    @Override
    public void deleteAmministratore(int id) throws DataException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM amministratore WHERE id = ?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataException("Errore nella cancellazione dell'amministratore", e);
        }
    }

    @Override
    public void storeAmministratore(Amministratore amministratore) throws DataException {
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "INSERT INTO amministratore (nome, cognome, cf, email, password, id_creatore, version) VALUES (?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, amministratore.getNome());
            stmt.setString(2, amministratore.getCognome());
            stmt.setString(3, amministratore.getCf());
            stmt.setString(4, amministratore.getEmail());
            stmt.setString(5, amministratore.getPassword());
            stmt.setInt(6, amministratore.getIdCreatore()); // ID del creatore
            stmt.setInt(7, amministratore.getVersion());

            int affectedRows = stmt.executeUpdate();

            // Recupera l'ID generato automaticamente
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        amministratore.setId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nell'inserimento dell'amministratore (" + e.getMessage() + ")", e);
        }
    }

    private Amministratore makeAmministratore(ResultSet rs) throws SQLException {
        // Usa lo stesso pattern dell'OperatoreProxy
        AmministratoreProxy admin = new AmministratoreProxy();
        admin.setDataLayer(dataLayer); // Imposta il DataLayer separatamente

        admin.setId(rs.getInt("id"));
        admin.setNome(rs.getString("nome"));
        admin.setCognome(rs.getString("cognome"));
        admin.setCf(rs.getString("cf"));
        admin.setEmail(rs.getString("email"));
        admin.setPassword(rs.getString("password"));
        admin.setIdCreatore(rs.getInt("id_creatore"));
        admin.setVersion(rs.getInt("version"));

        admin.setRuolo("amministratore");

        return admin;
    }
}
