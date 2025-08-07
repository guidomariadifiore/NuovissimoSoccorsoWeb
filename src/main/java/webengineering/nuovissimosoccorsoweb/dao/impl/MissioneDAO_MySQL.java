package webengineering.nuovissimosoccorsoweb.dao.impl;

import webengineering.framework.data.DataException;
import webengineering.framework.data.DataLayer;
import webengineering.nuovissimosoccorsoweb.dao.MissioneDAO;
import webengineering.nuovissimosoccorsoweb.model.Missione;
import webengineering.nuovissimosoccorsoweb.model.impl.proxy.MissioneProxy;
import webengineering.nuovissimosoccorsoweb.model.RuoloSquadra;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import webengineering.framework.data.DAO;
import webengineering.nuovissimosoccorsoweb.model.Materiale;
import webengineering.nuovissimosoccorsoweb.model.Mezzo;
import webengineering.nuovissimosoccorsoweb.model.PartecipazioneSquadra;
import webengineering.nuovissimosoccorsoweb.model.impl.proxy.MaterialeProxy;
import webengineering.nuovissimosoccorsoweb.model.impl.proxy.MezzoProxy;
import webengineering.nuovissimosoccorsoweb.model.impl.proxy.PartecipazioneSquadraProxy;

public class MissioneDAO_MySQL extends DAO implements MissioneDAO {

    public MissioneDAO_MySQL(DataLayer dataLayer) {
        super(dataLayer);
    }

    @Override
    public Missione getMissioneByCodice(int codiceRichiesta) throws DataException {
        Missione m = null;
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "SELECT * FROM missione WHERE codice_richiesta = ?")) {
            stmt.setInt(1, codiceRichiesta);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    m = makeMissione(rs);
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero della missione", e);
        }
        return m;
    }

    @Override
    public List<Missione> getAllMissioni() throws DataException {
        List<Missione> list = new ArrayList<>();
        try (Statement stmt = dataLayer.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM missione")) {
            while (rs.next()) {
                list.add(makeMissione(rs));
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero di tutte le missioni", e);
        }
        return list;
    }

    @Override
    public List<Missione> getMissioniInCorso() throws DataException {
        List<Missione> list = new ArrayList<>();
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "SELECT m.* FROM missione m INNER JOIN richiesta_soccorso r ON m.codice_richiesta = r.codice WHERE r.stato = 'Attiva'")) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(makeMissione(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero delle missioni in corso", e);
        }
        return list;
    }
    @Override
    public List<Missione> getMissioniByOperatore(int idOperatore) throws DataException {
        List<Missione> list = new ArrayList<>();
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "SELECT m.* FROM missione m JOIN squadra s ON m.codice_richiesta = s.codice_missione_assegnata WHERE s.id_op = ?")) {
            stmt.setInt(1, idOperatore);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(makeMissione(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero delle missioni per operatore", e);
        }
        return list;
    }

    @Override
    public void storeMissione(Missione missione) throws DataException {
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "INSERT INTO missione (codice_richiesta, nome, obiettivo, posizione, id_am, nota, data_ora_inizio, version) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setInt(1, missione.getCodiceRichiesta());
            stmt.setString(2, missione.getNome());
            stmt.setString(3, missione.getObiettivo());
            stmt.setString(4, missione.getPosizione());
            stmt.setInt(5, missione.getIdAmministratore());
            stmt.setString(6, missione.getNota());
            stmt.setTimestamp(7, Timestamp.valueOf(missione.getDataOraInizio()));
            stmt.setInt(8, missione.getVersion());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataException("Errore nell'inserimento della missione", e);
        }
    }

    @Override
    public void deleteMissione(int codiceRichiesta) throws DataException {
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "DELETE FROM missione WHERE codice_richiesta = ?")) {
            stmt.setInt(1, codiceRichiesta);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataException("Errore nella cancellazione della missione", e);
        }
    }
    
    @Override
    public void updateNoteMissione(int codiceRichiesta, String nota) throws DataException {
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "UPDATE missione SET nota = ? WHERE codice_richiesta = ?")) {
            stmt.setString(1, nota);
            stmt.setInt(2, codiceRichiesta);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new DataException("Nessuna missione trovata con codice: " + codiceRichiesta);
            }
        } catch (SQLException e) {
            throw new DataException("Errore nell'aggiornamento delle note della missione", e);
        }
    }

    private Missione makeMissione(ResultSet rs) throws SQLException {
        Missione m = new MissioneProxy();
        m.setCodiceRichiesta(rs.getInt("codice_richiesta"));
        m.setNome(rs.getString("nome"));
        m.setObiettivo(rs.getString("obiettivo"));
        m.setPosizione(rs.getString("posizione"));
        m.setNota(rs.getString("nota"));
        m.setIdAmministratore(rs.getInt("id_am"));

        // Gestione sicura della data
        Timestamp timestamp = rs.getTimestamp("data_ora_inizio");
        if (timestamp != null) {
            m.setDataOraInizio(timestamp.toLocalDateTime());
        } else {
            // Usa data corrente se NULL (debug in alcune situazioni scomode)
            m.setDataOraInizio(LocalDateTime.now());
        }

        m.setVersion(rs.getInt("version"));
        return m;
    }

@Override
public List<PartecipazioneSquadra> getSquadraByMissione(int codiceMissione) throws DataException {
    List<PartecipazioneSquadra> squadra = new ArrayList<>();
    try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
            "SELECT * FROM squadra WHERE codice_missione_assegnata = ?")) {
        stmt.setInt(1, codiceMissione);
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                PartecipazioneSquadra p = new PartecipazioneSquadraProxy();
                p.setIdOperatore(rs.getInt("id_op"));
                p.setCodiceMissione(rs.getInt("codice_missione_assegnata"));
                p.setRuolo(RuoloSquadra.valueOf(rs.getString("ruolo")));
                p.setVersion(1);
                squadra.add(p);
            }
        }
    } catch (SQLException e) {
        throw new DataException("Errore nel recupero della squadra per missione", e);
    }
    return squadra;
}

@Override
public void assegnaOperatoreAMissione(int idOperatore, int codiceMissione, String ruolo) throws DataException {
    // Controllo che ci sia almeno un Caposquadra se si aggiunge uno Standard
    if ("Standard".equalsIgnoreCase(ruolo)) {
        boolean caposquadraPresente = getSquadraByMissione(codiceMissione).stream()
                .anyMatch(p -> p.getRuolo() == RuoloSquadra.Caposquadra);
        if (!caposquadraPresente) {
            throw new DataException("Impossibile assegnare operatore Standard senza almeno un Caposquadra.");
        }
    }

    try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
            "INSERT INTO squadra (id_op, codice_missione_assegnata, ruolo) VALUES (?, ?, ?)")) {
        stmt.setInt(1, idOperatore);
        stmt.setInt(2, codiceMissione);
        stmt.setString(3, ruolo);
        stmt.executeUpdate();
    } catch (SQLException e) {
        throw new DataException("Errore nell'assegnazione dell'operatore alla missione", e);
    }
}

@Override
public void rimuoviOperatoreDaMissione(int idOperatore, int codiceMissione) throws DataException {
    try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
            "DELETE FROM squadra WHERE id_op = ? AND codice_missione_assegnata = ?")) {
        stmt.setInt(1, idOperatore);
        stmt.setInt(2, codiceMissione);
        stmt.executeUpdate();
    } catch (SQLException e) {
        throw new DataException("Errore nella rimozione dell'operatore dalla missione", e);
    }
}

@Override
public List<Mezzo> getMezziByMissione(int codiceMissione) throws DataException {
    List<Mezzo> mezzi = new ArrayList<>();
    try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
            "SELECT m.* FROM mezzo m JOIN utilizza_mezzo um ON m.targa = um.targa_mezzo WHERE um.codice_missione = ?")) {
        stmt.setInt(1, codiceMissione);
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Mezzo m = new MezzoProxy();
                m.setTarga(rs.getString("targa"));
                m.setDescrizione(rs.getString("descrizione"));
                m.setVersion(rs.getInt("version"));
                mezzi.add(m);
            }
        }
    } catch (SQLException e) {
        throw new DataException("Errore nel recupero dei mezzi per missione", e);
    }
    return mezzi;
}

@Override
public void assegnaMezzoAMissione(String targa, int codiceMissione) throws DataException {
    try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
            "INSERT INTO utilizza_mezzo (codice_missione, targa_mezzo) VALUES (?, ?)")) {
        stmt.setInt(1, codiceMissione);
        stmt.setString(2, targa);
        stmt.executeUpdate();
    } catch (SQLException e) {
        throw new DataException("Errore nell'assegnazione del mezzo alla missione", e);
    }
}

@Override
public void rimuoviMezzoDaMissione(String targa, int codiceMissione) throws DataException {
    try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
            "DELETE FROM utilizza_mezzo WHERE codice_missione = ? AND targa_mezzo = ?")) {
        stmt.setInt(1, codiceMissione);
        stmt.setString(2, targa);
        stmt.executeUpdate();
    } catch (SQLException e) {
        throw new DataException("Errore nella rimozione del mezzo dalla missione", e);
    }
}

@Override
public List<Materiale> getMaterialiByMissione(int codiceMissione) throws DataException {
    List<Materiale> materiali = new ArrayList<>();
    try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
            "SELECT m.* FROM materiale m JOIN utilizza_materiale um ON m.id = um.id_materiale WHERE um.codice_missione = ?")) {
        stmt.setInt(1, codiceMissione);
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Materiale m = new MaterialeProxy();
                m.setId(rs.getInt("id"));
                m.setNome(rs.getString("nome"));
                m.setDescrizione(rs.getString("descrizione"));
                m.setVersion(rs.getInt("version"));
                materiali.add(m);
            }
        }
    } catch (SQLException e) {
        throw new DataException("Errore nel recupero dei materiali per missione", e);
    }
    return materiali;
}

@Override
public void assegnaMaterialeAMissione(int idMateriale, int codiceMissione) throws DataException {
    try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
            "INSERT INTO utilizza_materiale (codice_missione, id_materiale) VALUES (?, ?)")) {
        stmt.setInt(1, codiceMissione);
        stmt.setInt(2, idMateriale);
        stmt.executeUpdate();
    } catch (SQLException e) {
        throw new DataException("Errore nell'assegnazione del materiale alla missione", e);
    }
}

@Override
public void rimuoviMaterialeDaMissione(int idMateriale, int codiceMissione) throws DataException {
    try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
            "DELETE FROM utilizza_materiale WHERE codice_missione = ? AND id_materiale = ?")) {
        stmt.setInt(1, codiceMissione);
        stmt.setInt(2, idMateriale);
        stmt.executeUpdate();
    } catch (SQLException e) {
        throw new DataException("Errore nella rimozione del materiale dalla missione", e);
    }
}
    
    @Override
    public List<Missione> getMissioniInCorsoByOperatore
    (int idOperatore) throws DataException {
        List<Missione> list = new ArrayList<>();
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "SELECT m.* FROM missione m "
                + "JOIN squadra s ON m.codice_richiesta = s.codice_missione_assegnata "
                + "JOIN richiesta_soccorso r ON m.codice_richiesta = r.codice "
                + "WHERE s.id_op = ? AND r.stato = 'Attiva'")) {
            stmt.setInt(1, idOperatore);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(makeMissione(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero delle missioni in corso per operatore", e);
        }
        return list;
    }
    @Override
    public int countMissioniInCorsoByOperatore
    (int idOperatore) throws DataException {
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "SELECT COUNT(*) FROM missione m "
                + "JOIN squadra s ON m.codice_richiesta = s.codice_missione_assegnata "
                + "JOIN richiesta_soccorso r ON m.codice_richiesta = r.codice "
                + "WHERE s.id_op = ? AND r.stato = 'Attiva'")) {
            stmt.setInt(1, idOperatore);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel conteggio delle missioni in corso per operatore", e);
        }
        return 0;
    }

    @Override
    public int countMissioniCompletateByOperatore
    (int idOperatore) throws DataException {
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "SELECT COUNT(*) FROM info_missione im "
                + "JOIN missione m ON im.codice_missione = m.codice_richiesta "
                + "JOIN squadra s ON m.codice_richiesta = s.codice_missione_assegnata "
                + "WHERE s.id_op = ?")) {
            stmt.setInt(1, idOperatore);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel conteggio delle missioni completate per operatore", e);
        }
        return 0;
    }
}
