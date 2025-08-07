package webengineering.nuovissimosoccorsoweb.dao.impl;

import webengineering.framework.data.DataException;
import webengineering.framework.data.DataLayer;
import webengineering.nuovissimosoccorsoweb.dao.MaterialeDAO;
import webengineering.nuovissimosoccorsoweb.model.Materiale;
import webengineering.nuovissimosoccorsoweb.model.impl.proxy.MaterialeProxy;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import webengineering.framework.data.DAO;

public class MaterialeDAO_MySQL extends DAO implements MaterialeDAO {


    public MaterialeDAO_MySQL(DataLayer dataLayer) {
        super(dataLayer);
    }

    @Override
    public Materiale getMaterialeById(int id) throws DataException {
        Materiale materiale = null;

        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "SELECT * FROM materiale WHERE id = ?")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    materiale = makeMateriale(rs);
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero del materiale", e);
        }

        return materiale;
    }

    @Override
    public List<Materiale> getAllMateriali() throws DataException {
        List<Materiale> result = new ArrayList<>();

        try (Statement stmt = dataLayer.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM materiale")) {
            while (rs.next()) {
                result.add(makeMateriale(rs));
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero della lista materiali", e);
        }

        return result;
    }

    @Override
    public void storeMateriale(Materiale materiale) throws DataException {
        if (materiale.getId() > 0) {
            // UPDATE
            try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                    "UPDATE materiale SET nome = ?, descrizione = ?, version = ? WHERE id = ?")) {
                stmt.setString(1, materiale.getNome());
                stmt.setString(2, materiale.getDescrizione());
                stmt.setInt(3, materiale.getVersion());
                stmt.setInt(4, materiale.getId());
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new DataException("Errore nell'aggiornamento del materiale", e);
            }
        } else {
            // INSERT
            try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                    "INSERT INTO materiale (nome, descrizione, version) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, materiale.getNome());
                stmt.setString(2, materiale.getDescrizione());
                stmt.setInt(3, materiale.getVersion());
                stmt.executeUpdate();

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        materiale.setId(keys.getInt(1));
                    }
                }
            } catch (SQLException e) {
                throw new DataException("Errore nell'inserimento del materiale", e);
            }
        }
    }

    @Override
    public void deleteMateriale(int id) throws DataException {
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "DELETE FROM materiale WHERE id = ?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataException("Errore nella cancellazione del materiale", e);
        }
    }

    @Override
    public List<Materiale> getMaterialiDisponibili() throws DataException {
        List<Materiale> result = new ArrayList<>();
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "SELECT ma.* FROM materiale ma WHERE ma.id NOT IN ("
                + "SELECT DISTINCT uma.id_materiale FROM utilizza_materiale uma "
                + "INNER JOIN missione mi ON uma.codice_missione = mi.codice_richiesta "
                + "INNER JOIN richiesta_soccorso r ON mi.codice_richiesta = r.codice "
                + "WHERE r.stato = 'Attiva')")) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(makeMateriale(rs)); 
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero dei materiali disponibili", e);
        }
        return result;
    }
    @Override
    public List<Materiale> getMaterialiByMissione(int codiceMissione) throws DataException {
        List<Materiale> result = new ArrayList<>();

        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "SELECT m.* FROM materiale m JOIN utilizza_materiale um ON m.id = um.id_materiale WHERE um.codice_missione = ?")) {
            stmt.setInt(1, codiceMissione);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(makeMateriale(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero dei materiali per missione", e);
        }

        return result;
    }

    private Materiale makeMateriale(ResultSet rs) throws SQLException {
        Materiale materiale = new MaterialeProxy();
        materiale.setId(rs.getInt("id"));
        materiale.setNome(rs.getString("nome"));
        materiale.setDescrizione(rs.getString("descrizione"));
        materiale.setVersion(rs.getInt("version"));
        return materiale;
    }
}
