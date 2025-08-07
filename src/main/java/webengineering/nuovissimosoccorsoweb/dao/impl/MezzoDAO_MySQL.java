package webengineering.nuovissimosoccorsoweb.dao.impl;

import webengineering.framework.data.DataException;
import webengineering.framework.data.DataLayer;
import webengineering.nuovissimosoccorsoweb.dao.MezzoDAO;
import webengineering.nuovissimosoccorsoweb.model.Mezzo;
import webengineering.nuovissimosoccorsoweb.model.impl.proxy.MezzoProxy;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import webengineering.framework.data.DAO;

public class MezzoDAO_MySQL extends DAO implements MezzoDAO {

    public MezzoDAO_MySQL(DataLayer dataLayer) {
        super(dataLayer);
    }

    @Override
    public Mezzo getMezzoByTarga(String targa) throws DataException {
        Mezzo mezzo = null;

        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "SELECT * FROM mezzo WHERE targa = ?")) {
            stmt.setString(1, targa);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    mezzo = makeMezzo(rs);
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero del mezzo", e);
        }

        return mezzo;
    }

    @Override
    public List<Mezzo> getAllMezzi() throws DataException {
        List<Mezzo> mezzi = new ArrayList<>();

        try (Statement stmt = dataLayer.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM mezzo")) {
            while (rs.next()) {
                mezzi.add(makeMezzo(rs));
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero della lista mezzi", e);
        }

        return mezzi;
    }

    @Override
    public List<Mezzo> getMezziDisponibili() throws DataException {
        List<Mezzo> result = new ArrayList<>();
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "SELECT m.* FROM mezzo m WHERE m.targa NOT IN ("
                + "SELECT DISTINCT um.targa_mezzo FROM utilizza_mezzo um "
                + "INNER JOIN missione mi ON um.codice_missione = mi.codice_richiesta "
                + "INNER JOIN richiesta_soccorso r ON mi.codice_richiesta = r.codice "
                + "WHERE r.stato = 'Attiva')")) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(makeMezzo(rs)); // Assumendo che esista questo metodo
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero dei mezzi disponibili", e);
        }
        return result;
    }
    
    @Override
    public List<Mezzo> getMezziByMissione(int codiceMissione) throws DataException {
        List<Mezzo> mezzi = new ArrayList<>();

        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "SELECT m.* FROM mezzo m JOIN utilizza_mezzo um ON m.targa = um.targa_mezzo WHERE um.codice_missione = ?")) {
            stmt.setInt(1, codiceMissione);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    mezzi.add(makeMezzo(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero dei mezzi per missione", e);
        }

        return mezzi;
    }

    @Override
    public void storeMezzo(Mezzo mezzo) throws DataException {
        if (getMezzoByTarga(mezzo.getTarga()) != null) {
            // UPDATE - Aggiornato per includere il nome
            try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                    "UPDATE mezzo SET nome = ?, descrizione = ?, version = ? WHERE targa = ?")) {
                stmt.setString(1, mezzo.getNome());
                stmt.setString(2, mezzo.getDescrizione());
                stmt.setInt(3, mezzo.getVersion());
                stmt.setString(4, mezzo.getTarga());
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new DataException("Errore nell'aggiornamento del mezzo", e);
            }
        } else {
            // INSERT - Corretto per includere il nome
            try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                    "INSERT INTO mezzo (targa, nome, descrizione, version) VALUES (?, ?, ?, ?)")) {
                stmt.setString(1, mezzo.getTarga());
                stmt.setString(2, mezzo.getNome()); // <- Campo mancante aggiunto
                stmt.setString(3, mezzo.getDescrizione());
                stmt.setInt(4, mezzo.getVersion());
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new DataException("Errore nell'inserimento del mezzo (Field 'nome' doesn't have a default value)", e);
            }
        }
    }

    @Override
    public void deleteMezzo(String targa) throws DataException {
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "DELETE FROM mezzo WHERE targa = ?")) {
            stmt.setString(1, targa);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataException("Errore nella cancellazione del mezzo", e);
        }
    }

    // Aggiornato il metodo makeMezzo per includere il nome
    private Mezzo makeMezzo(ResultSet rs) throws SQLException {
        Mezzo m = new MezzoProxy();
        m.setTarga(rs.getString("targa"));
        m.setNome(rs.getString("nome")); // <- Campo mancante aggiunto
        m.setDescrizione(rs.getString("descrizione"));
        m.setVersion(rs.getInt("version"));
        return m;
    }
}