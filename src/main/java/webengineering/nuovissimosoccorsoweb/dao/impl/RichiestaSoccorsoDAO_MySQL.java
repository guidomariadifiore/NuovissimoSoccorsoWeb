package webengineering.nuovissimosoccorsoweb.dao.impl;

import webengineering.framework.data.DataException;
import webengineering.framework.data.DataLayer;
import webengineering.nuovissimosoccorsoweb.model.RichiestaSoccorso;
import webengineering.nuovissimosoccorsoweb.model.impl.proxy.RichiestaSoccorsoProxy;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import webengineering.framework.data.DAO;
import webengineering.nuovissimosoccorsoweb.dao.RichiestaSoccorsoDAO;

public class RichiestaSoccorsoDAO_MySQL extends DAO implements RichiestaSoccorsoDAO {

    public RichiestaSoccorsoDAO_MySQL(DataLayer dataLayer) {
        super(dataLayer);
    }

    private RichiestaSoccorso makeRichiesta(ResultSet rs) throws SQLException {
        RichiestaSoccorso r = new RichiestaSoccorsoProxy();
        r.setCodice(rs.getInt("codice"));
        r.setStato(rs.getString("stato"));
        r.setCoordinate(rs.getString("coordinate"));
        r.setIndirizzo(rs.getString("indirizzo"));
        r.setDescrizione(rs.getString("descrizione"));
        r.setStringa(rs.getString("stringa"));
        r.setNome(rs.getString("nome"));
        r.setFoto(rs.getString("foto"));
        r.setIp(rs.getString("ip"));
        r.setEmailSegnalante(rs.getString("email_s"));
        r.setNomeSegnalante(rs.getString("nome_s"));
        r.setIdAmministratore(rs.getInt("id_am"));
        return r;
    }

    @Override
    public RichiestaSoccorso getRichiestaByCodice(int codice) throws DataException {
        RichiestaSoccorso r = null;
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "SELECT * FROM richiesta_soccorso WHERE codice = ?")) {
            stmt.setInt(1, codice);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    r = makeRichiesta(rs);
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero della richiesta", e);
        }
        return r;
    }

    @Override
    public List<RichiestaSoccorso> getAllRichieste() throws DataException {
        List<RichiestaSoccorso> result = new ArrayList<>();
        try (Statement stmt = dataLayer.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM richiesta_soccorso")) {
            while (rs.next()) {
                result.add(makeRichiesta(rs));
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero delle richieste", e);
        }
        return result;
    }

    @Override
    public List<RichiestaSoccorso> getRichiesteByStato(String stato) throws DataException {
        List<RichiestaSoccorso> result = new ArrayList<>();
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "SELECT * FROM richiesta_soccorso WHERE stato = ?")) {
            stmt.setString(1, stato);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(makeRichiesta(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero richieste per stato", e);
        }
        return result;
    }

    @Override
    public List<RichiestaSoccorso> getRichiesteByAmministratore(int idAmministratore) throws DataException {
        List<RichiestaSoccorso> result = new ArrayList<>();
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "SELECT * FROM richiesta_soccorso WHERE id_am = ?")) {
            stmt.setInt(1, idAmministratore);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(makeRichiesta(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero richieste per amministratore", e);
        }
        return result;
    }

    @Override
    public List<RichiestaSoccorso> getRichiesteConvalidateNonGestite() throws DataException {
        List<RichiestaSoccorso> result = new ArrayList<>();
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "SELECT * FROM richiesta_soccorso WHERE stato = 'Convalidata' AND codice NOT IN (SELECT codice_richiesta FROM missione)")) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(makeRichiesta(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero richieste convalidate non gestite", e);
        }
        return result;
    }

    @Override
    public RichiestaSoccorso getRichiestaByStringaValidazione(String stringa) throws DataException {
        RichiestaSoccorso r = null;
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "SELECT * FROM richiesta_soccorso WHERE stringa = ?")) {
            stmt.setString(1, stringa);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    r = makeRichiesta(rs);
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero richiesta per validazione", e);
        }
        return r;
    }
    
    @Override
    public void updateStato(int codice, String nuovoStato) throws DataException {
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "UPDATE richiesta_soccorso SET stato = ? WHERE codice = ?")) {
            stmt.setString(1, nuovoStato);
            stmt.setInt(2, codice);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new DataException("Nessuna richiesta trovata con codice: " + codice);
            }

        } catch (SQLException e) {
            throw new DataException("Errore nell'aggiornamento dello stato della richiesta", e);
        }
    }


    @Override
    public void storeRichiesta(RichiestaSoccorso richiesta) throws DataException {
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "INSERT INTO richiesta_soccorso (stato, coordinate, indirizzo, descrizione, stringa, nome, foto, ip, email_s, nome_s, id_am) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, richiesta.getStato());
            stmt.setString(2, richiesta.getCoordinate());
            stmt.setString(3, richiesta.getIndirizzo());
            stmt.setString(4, richiesta.getDescrizione());
            stmt.setString(5, richiesta.getStringa());
            stmt.setString(6, richiesta.getNome());
            stmt.setString(7, richiesta.getFoto());
            stmt.setString(8, richiesta.getIp());
            stmt.setString(9, richiesta.getEmailSegnalante());
            stmt.setString(10, richiesta.getNomeSegnalante());
            if (richiesta.getIdAmministratore() > 0) {
                stmt.setInt(11, richiesta.getIdAmministratore());
            } else {
                stmt.setNull(11, Types.INTEGER);
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataException("Errore nel salvataggio richiesta", e);
        }
    }

    @Override
    public void deleteRichiesta(int codice) throws DataException {
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "DELETE FROM richiesta_soccorso WHERE codice = ?")) {
            stmt.setInt(1, codice);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataException("Errore nell'eliminazione richiesta", e);
        }
    }
}
