package webengineering.nuovissimosoccorsoweb.dao.impl;

import webengineering.framework.data.DataException;
import webengineering.framework.data.DataLayer;
import webengineering.nuovissimosoccorsoweb.dao.OperatoreDAO;
import webengineering.nuovissimosoccorsoweb.model.Operatore;
import webengineering.nuovissimosoccorsoweb.model.impl.proxy.OperatoreProxy;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import webengineering.framework.data.DAO;

public class OperatoreDAO_MySQL extends DAO implements OperatoreDAO {

    public OperatoreDAO_MySQL(DataLayer dataLayer) {
        super(dataLayer);
    }

    @Override
    public Operatore getOperatoreById(int id) throws DataException {
        Operatore o = null;
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "SELECT * FROM operatore WHERE id = ?")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    o = makeOperatore(rs);
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero dell'operatore per ID", e);
        }
        return o;
    }

    @Override
    public Operatore getOperatoreByEmail(String email) throws DataException {
        Operatore o = null;
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "SELECT * FROM operatore WHERE email = ?")) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    o = makeOperatore(rs);
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero dell'operatore per email", e);
        }
        return o;
    }

    @Override
    public List<Operatore> getAllOperatori() throws DataException {
        List<Operatore> list = new ArrayList<>();
        try (Statement stmt = dataLayer.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM operatore")) {
            while (rs.next()) {
                list.add(makeOperatore(rs));
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero di tutti gli operatori", e);
        }
        return list;
    }
    
    @Override
    public Operatore getOperatoreByCf (String cf) throws DataException {
        Operatore o = null;

        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM operatore WHERE cf = ?")) {
            stmt.setString(1, cf);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    o = makeOperatore(rs);
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero dell'dell'operatore per codice fiscale", e);
        }

        return o;
    }

    @Override
    public List<Operatore> getOperatoriDisponibili() throws DataException {
        List<Operatore> result = new ArrayList<>();
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "SELECT o.* FROM operatore o WHERE o.id NOT IN ("
                + "SELECT DISTINCT s.id_op FROM squadra s "
                + "INNER JOIN missione m ON s.codice_missione_assegnata = m.codice_richiesta "
                + "INNER JOIN richiesta_soccorso r ON m.codice_richiesta = r.codice "
                + "WHERE r.stato = 'Attiva')")) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(makeOperatore(rs)); // Assumendo che esista questo metodo
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero degli operatori disponibili", e);
        }
        return result;
    }
    @Override
    public void storeOperatore(Operatore operatore) throws DataException {
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "INSERT INTO operatore (nome, cognome, email, password, cf, id_creatore, version) VALUES (?, ?, ?, ?, ?, ?, ?)", 
                Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, operatore.getNome());
            stmt.setString(2, operatore.getCognome());
            stmt.setString(3, operatore.getEmail());
            stmt.setString(4, operatore.getPassword());
            stmt.setString(5, operatore.getCodiceFiscale());
            stmt.setInt(6, operatore.getIdAmministratore()); // Questo va nella colonna id_creatore
            stmt.setInt(7, operatore.getVersion());
            
            int affectedRows = stmt.executeUpdate();
            
            // Recupera l'ID generato automaticamente
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        operatore.setId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nell'inserimento dell'operatore (" + e.getMessage() + ")", e);
        }
    }

    @Override
    public void deleteOperatore(int id) throws DataException {
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "DELETE FROM operatore WHERE id = ?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataException("Errore nella cancellazione dell'operatore", e);
        }
    }

    private Operatore makeOperatore(ResultSet rs) throws SQLException {
        Operatore op = new OperatoreProxy();
        op.setId(rs.getInt("id"));
        op.setNome(rs.getString("nome"));
        op.setCognome(rs.getString("cognome"));
        op.setEmail(rs.getString("email"));
        op.setPassword(rs.getString("password"));
        op.setCodiceFiscale(rs.getString("cf"));
        op.setIdAmministratore(rs.getInt("id_creatore")); 
        op.setVersion(rs.getInt("version"));
        return op;
    }
}