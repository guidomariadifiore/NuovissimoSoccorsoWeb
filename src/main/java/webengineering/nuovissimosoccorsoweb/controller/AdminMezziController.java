package webengineering.nuovissimosoccorsoweb.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.*;
import webengineering.framework.result.TemplateManagerException;
import webengineering.nuovissimosoccorsoweb.SoccorsoDataLayer;
import webengineering.framework.data.DataException;
import webengineering.nuovissimosoccorsoweb.model.Mezzo;

public class AdminMezziController extends SoccorsoBaseController {
    
    private static final Logger logger = Logger.getLogger(AdminMezziController.class.getName());
    
    @Override
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        // Verifica che l'utente sia un amministratore
        if (!isUserAdmin(request)) {
            try {
                response.sendRedirect(request.getContextPath() + "/login?error=access_denied");
                return;
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Errore nel redirect per accesso negato", ex);
                handleError(ex, request, response);
                return;
            }
        }
        
        try {
            // Ottieni il DataLayer
            SoccorsoDataLayer dataLayer = (SoccorsoDataLayer) request.getAttribute("datalayer");
            if (dataLayer == null) {
                logger.severe("DataLayer non disponibile nella request");
                try {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore di sistema");
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Errore critico", e);
                }
                return;
            }
            
            // Controlla se è una richiesta per dettagli mezzo specifico
            String pathInfo = request.getPathInfo();
            if (pathInfo != null && pathInfo.length() > 1) {
                // Estrai la targa dal path (rimuove il primo "/")
                String targa = pathInfo.substring(1);
                showDettagliMezzo(request, response, targa, dataLayer);
            } else {
                // Carica tutti i mezzi per la lista
                List<Mezzo> mezzi = loadAllMezzi(dataLayer);
                
                // Mostra la vista dei mezzi
                showMezziView(request, response, mezzi, dataLayer);
            }
            
        } catch (DataException ex) {
            logger.log(Level.SEVERE, "Errore database nel caricamento mezzi", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nel caricamento dei dati");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico nel caricamento mezzi", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore di sistema");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        }
    }
    
    // Verifica se l'utente è un amministratore
    private boolean isUserAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        
        String userRole = (String) session.getAttribute("user_role");
        return "admin".equals(userRole);
    }
    
    // Carica tutti i mezzi dal database
    private List<Mezzo> loadAllMezzi(SoccorsoDataLayer dataLayer) throws DataException {
        return dataLayer.getMezzoDAO().getAllMezzi();
    }
    
    // Mostra la vista con la lista dei mezzi
    private void showMezziView(HttpServletRequest request, HttpServletResponse response, 
                              List<Mezzo> mezzi, SoccorsoDataLayer dataLayer) throws ServletException {
        
        try {
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("thispageurl", request.getAttribute("thispageurl"));
            dataModel.put("outline_tpl", null);
            
            addUserInfoToModel(request, dataModel);
            
            // Aggiungi context path
            dataModel.put("contextPath", request.getContextPath());
            
            // Gestisci messaggi di successo/errore
            String successParam = request.getParameter("success");
            String errorParam = request.getParameter("error");
            
            if (successParam != null && !successParam.trim().isEmpty()) {
                dataModel.put("success_message", getSuccessMessage(successParam));
            }
            
            if (errorParam != null && !errorParam.trim().isEmpty()) {
                dataModel.put("error_message", getErrorMessage(errorParam));
            }
            
            // Crea una mappa dei mezzi con il loro stato di disponibilità
            Map<String, Boolean> mezziDisponibilita = new HashMap<>();
            List<Mezzo> mezziDisponibili = dataLayer.getMezzoDAO().getMezziDisponibili();
            
            for (Mezzo mezzo : mezzi) {
                boolean disponibile = mezziDisponibili.stream()
                    .anyMatch(mezzoDisp -> mezzoDisp.getTarga().equals(mezzo.getTarga()));
                mezziDisponibilita.put(mezzo.getTarga(), disponibile);
            }
            
            // Aggiungi la lista dei mezzi e la loro disponibilità
            dataModel.put("mezzi", mezzi);
            dataModel.put("mezzi_disponibilita", mezziDisponibilita);
            dataModel.put("total_mezzi", mezzi.size());
            
            // Calcola statistiche disponibilità
            long mezziDisponibiliCount = mezziDisponibilita.values().stream()
                .mapToLong(disponibile -> disponibile ? 1L : 0L).sum();
            long mezziInUsoCount = mezzi.size() - mezziDisponibiliCount;
            
            dataModel.put("mezzi_disponibili_count", mezziDisponibiliCount);
            dataModel.put("mezzi_in_uso_count", mezziInUsoCount);
            
            // Informazioni admin
            HttpSession session = request.getSession(false);
            if (session != null) {
                String fullName = (String) session.getAttribute("full_name");
                dataModel.put("admin_name", fullName != null ? fullName : "Amministratore");
            }
            
            // Carica il template
            new webengineering.framework.result.TemplateResult(getServletContext())
                    .activate("admin_mezzi.ftl.html", dataModel, response);
                    
        } catch (TemplateManagerException ex) {
            logger.log(Level.SEVERE, "Errore nel template dei mezzi", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nel caricamento della pagina");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        } catch (DataException ex) {
            logger.log(Level.SEVERE, "Errore nel caricamento dati disponibilità mezzi", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nel caricamento dei dati");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        }
    }
    
    // Mostra i dettagli di un mezzo specifico
    private void showDettagliMezzo(HttpServletRequest request, HttpServletResponse response, 
                                  String targa, SoccorsoDataLayer dataLayer) throws ServletException {
        
        try {
            // Carica il mezzo
            Mezzo mezzo = dataLayer.getMezzoDAO().getMezzoByTarga(targa);
            if (mezzo == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Mezzo non trovato");
                return;
            }
            
            // Carica storico missioni del mezzo
            List<webengineering.nuovissimosoccorsoweb.model.Missione> storicoMissioni = 
                getMissioniByMezzo(targa, dataLayer);
            
            logger.info("Trovate " + storicoMissioni.size() + " missioni per mezzo " + targa);
            
            // Per ogni missione, carica anche le informazioni della richiesta di soccorso e info conclusione
            List<MissioneConDettagli> missioniConDettagli = new ArrayList<>();
            for (webengineering.nuovissimosoccorsoweb.model.Missione missione : storicoMissioni) {
                try {
                    // Carica richiesta di soccorso collegata
                    webengineering.nuovissimosoccorsoweb.model.RichiestaSoccorso richiesta = 
                        dataLayer.getRichiestaSoccorsoDAO().getRichiestaByCodice(missione.getCodiceRichiesta());
                    
                    // Carica info missione (data fine, successo, commento)
                    webengineering.nuovissimosoccorsoweb.model.InfoMissione infoMissione = null;
                    try {
                        infoMissione = dataLayer.getInfoMissioneDAO().getInfoByCodiceMissione(missione.getCodiceRichiesta());
                    } catch (DataException ex) {
                        logger.log(Level.WARNING, "InfoMissione non trovata per missione " + missione.getCodiceRichiesta(), ex);
                        // infoMissione rimane null se non trovata
                    }
                    
                    missioniConDettagli.add(new MissioneConDettagli(missione, richiesta, infoMissione));
                } catch (DataException ex) {
                    logger.log(Level.WARNING, "Errore nel caricamento dettagli per missione " + missione.getCodiceRichiesta(), ex);
                    // Aggiungi la missione comunque, ma senza dettagli (saranno null)
                    missioniConDettagli.add(new MissioneConDettagli(missione, null, null));
                }
            }
            
            // Determina se è disponibile
            List<Mezzo> mezziDisponibili = dataLayer.getMezzoDAO().getMezziDisponibili();
            boolean disponibile = mezziDisponibili.stream()
                .anyMatch(mezzoDisp -> mezzoDisp.getTarga().equals(targa));
            
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("thispageurl", request.getAttribute("thispageurl"));
            dataModel.put("outline_tpl", null);
            
            addUserInfoToModel(request, dataModel);
            
            // Aggiungi context path
            dataModel.put("contextPath", request.getContextPath());
            
            // Gestisci messaggi
            String successParam = request.getParameter("success");
            String errorParam = request.getParameter("error");
            
            if (successParam != null && !successParam.trim().isEmpty()) {
                dataModel.put("success_message", getSuccessMessage(successParam));
            }
            
            if (errorParam != null && !errorParam.trim().isEmpty()) {
                dataModel.put("error_message", getErrorMessage(errorParam));
            }
            
            // Aggiungi dati mezzo
            dataModel.put("mezzo", mezzo);
            dataModel.put("disponibile", disponibile);
            dataModel.put("storico_missioni", missioniConDettagli);
            
            // Informazioni admin
            HttpSession session = request.getSession(false);
            if (session != null) {
                String fullName = (String) session.getAttribute("full_name");
                dataModel.put("admin_name", fullName != null ? fullName : "Amministratore");
            }
            
            // Carica il template per i dettagli
            new webengineering.framework.result.TemplateResult(getServletContext())
                    .activate("admin_mezzo_dettagli.ftl.html", dataModel, response);
                    
        } catch (TemplateManagerException ex) {
            logger.log(Level.SEVERE, "Errore nel template dettagli mezzo", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nel caricamento della pagina");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        } catch (DataException ex) {
            logger.log(Level.SEVERE, "Errore nel caricamento dettagli mezzo", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nel caricamento dei dati");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico nei dettagli mezzo", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore di sistema");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        }
    }
    
    // Metodo per ottenere tutte le missioni a cui ha partecipato un mezzo
    private List<webengineering.nuovissimosoccorsoweb.model.Missione> getMissioniByMezzo(String targa, SoccorsoDataLayer dataLayer) throws DataException {
        List<webengineering.nuovissimosoccorsoweb.model.Missione> missioni = new ArrayList<>();
        
        // Query per ottenere tutte le missioni che hanno utilizzato questo mezzo
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "SELECT m.* FROM missione m " +
                "JOIN utilizza_mezzo um ON m.codice_richiesta = um.codice_missione " +
                "WHERE um.targa_mezzo = ? " +
                "ORDER BY m.data_ora_inizio DESC")) {
            
            stmt.setString(1, targa);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    webengineering.nuovissimosoccorsoweb.model.Missione missione = 
                        new webengineering.nuovissimosoccorsoweb.model.impl.proxy.MissioneProxy();
                    missione.setCodiceRichiesta(rs.getInt("codice_richiesta"));
                    missione.setNome(rs.getString("nome"));
                    missione.setObiettivo(rs.getString("obiettivo"));
                    missione.setPosizione(rs.getString("posizione"));
                    missione.setNota(rs.getString("nota"));
                    missione.setIdAmministratore(rs.getInt("id_am"));
                    
                    // Gestione sicura della data
                    Timestamp timestamp = rs.getTimestamp("data_ora_inizio");
                    if (timestamp != null) {
                        missione.setDataOraInizio(timestamp.toLocalDateTime());
                    } else {
                        missione.setDataOraInizio(java.time.LocalDateTime.now());
                    }
                    
                    missione.setVersion(rs.getInt("version"));
                    missioni.add(missione);
                }
            }
        } catch (SQLException e) {
            throw new DataException("Errore nel recupero delle missioni per mezzo", e);
        }
        
        return missioni;
    }
    
    // Messaggi di successo
    private String getSuccessMessage(String successCode) {
        switch (successCode) {
            case "mezzo_created":
                return "Mezzo creato con successo.";
            case "mezzo_updated":
                return "Mezzo aggiornato con successo.";
            case "mezzo_deleted":
                return "Mezzo eliminato con successo.";
            default:
                return "Operazione completata con successo.";
        }
    }
    
    // Messaggi di errore
    private String getErrorMessage(String errorCode) {
        switch (errorCode) {
            case "access_denied":
                return "Accesso negato. Solo gli amministratori possono accedere.";
            case "database_error":
                return "Errore nel database. Riprova più tardi.";
            case "mezzo_not_found":
                return "Mezzo non trovato.";
            case "validation_error":
                return "Dati non validi. Controlla i campi inseriti.";
            default:
                return "Si è verificato un errore.";
        }
    }
    
    // Classe di supporto per contenere missione + dettagli richiesta + info conclusione (per mezzi)
    public static class MissioneConDettagli {
        private final webengineering.nuovissimosoccorsoweb.model.Missione missione;
        private final webengineering.nuovissimosoccorsoweb.model.RichiestaSoccorso richiesta;
        private final webengineering.nuovissimosoccorsoweb.model.InfoMissione infoMissione;
        
        public MissioneConDettagli(webengineering.nuovissimosoccorsoweb.model.Missione missione, 
                                  webengineering.nuovissimosoccorsoweb.model.RichiestaSoccorso richiesta, 
                                  webengineering.nuovissimosoccorsoweb.model.InfoMissione infoMissione) {
            this.missione = missione;
            this.richiesta = richiesta;
            this.infoMissione = infoMissione;
        }
        
        // Getter pubblici per Freemarker
        public webengineering.nuovissimosoccorsoweb.model.Missione getMissione() {
            return missione;
        }
        
        public webengineering.nuovissimosoccorsoweb.model.RichiestaSoccorso getRichiesta() {
            return richiesta;
        }
        
        public webengineering.nuovissimosoccorsoweb.model.InfoMissione getInfoMissione() {
            return infoMissione;
        }
    }
}