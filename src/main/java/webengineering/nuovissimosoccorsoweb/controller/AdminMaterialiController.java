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
import webengineering.nuovissimosoccorsoweb.model.Materiale;

public class AdminMaterialiController extends SoccorsoBaseController {
    
    private static final Logger logger = Logger.getLogger(AdminMaterialiController.class.getName());
    
    @Override
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        logger.info("=== AdminMaterialiController - processRequest chiamato ===");
        logger.info("Metodo HTTP: " + request.getMethod());
        logger.info("Context Path: " + request.getContextPath());
        logger.info("Request URI: " + request.getRequestURI());
        
        // Verifica che l'utente sia un amministratore
        if (!isUserAdmin(request)) {
            logger.warning("Accesso negato - utente non admin");
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
            
            // Controlla se è una richiesta per dettagli materiale specifico
            String pathInfo = request.getPathInfo();
            if (pathInfo != null && pathInfo.length() > 1) {
                // Estrai l'ID dal path (rimuove il primo "/")
                String idStr = pathInfo.substring(1);
                try {
                    int idMateriale = Integer.parseInt(idStr);
                    showDettagliMateriale(request, response, idMateriale, dataLayer);
                } catch (NumberFormatException e) {
                    logger.warning("ID materiale non valido: " + idStr);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID materiale non valido");
                }
            } else {
                // Carica tutti i materiali per la lista
                List<Materiale> materiali = loadAllMateriali(dataLayer);
                
                // Mostra la vista dei materiali
                showMaterialiView(request, response, materiali, dataLayer);
            }
            
        } catch (DataException ex) {
            logger.log(Level.SEVERE, "Errore database nel caricamento materiali", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nel caricamento dei dati");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico nel caricamento materiali", ex);
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
        if (session == null) {
            logger.warning("Nessuna sessione trovata");
            return false;
        }
        
        String userRole = (String) session.getAttribute("user_role");
        String userType = (String) session.getAttribute("user_type");
        
        logger.info("User role dalla sessione: " + userRole);
        logger.info("User type dalla sessione: " + userType);
        
        boolean isAdmin = "admin".equals(userRole) || "amministratore".equals(userType);
        logger.info("È admin? " + isAdmin);
        
        return isAdmin;
    }
    
    // Carica tutti i materiali dal database
    private List<Materiale> loadAllMateriali(SoccorsoDataLayer dataLayer) throws DataException {
        logger.info("Caricamento di tutti i materiali");
        return dataLayer.getMaterialeDAO().getAllMateriali();
    }
    
    // Mostra la vista con la lista dei materiali
    private void showMaterialiView(HttpServletRequest request, HttpServletResponse response, 
                                  List<Materiale> materiali, SoccorsoDataLayer dataLayer) throws ServletException {
        
        logger.info("=== showMaterialiView ===");
        
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
            
            // Crea una mappa dei materiali con il loro stato di disponibilità
            Map<String, Boolean> materialiDisponibilita = new HashMap<>();
            List<Materiale> materialiDisponibili = dataLayer.getMaterialeDAO().getMaterialiDisponibili();
            
            for (Materiale materiale : materiali) {
                boolean disponibile = materialiDisponibili.stream()
                    .anyMatch(matDisp -> matDisp.getId() == materiale.getId());
                materialiDisponibilita.put(String.valueOf(materiale.getId()), disponibile);
            }
            
            // Aggiungi la lista dei materiali e la loro disponibilità
            dataModel.put("materiali", materiali);
            dataModel.put("materiali_disponibilita", materialiDisponibilita);
            dataModel.put("total_materiali", materiali.size());
            
            // Calcola statistiche disponibilità
            long materialiDisponibiliCount = materialiDisponibilita.values().stream()
                .mapToLong(disponibile -> disponibile ? 1L : 0L).sum();
            long materialiInUsoCount = materiali.size() - materialiDisponibiliCount;
            
            dataModel.put("materiali_disponibili_count", materialiDisponibiliCount);
            dataModel.put("materiali_in_uso_count", materialiInUsoCount);
            
            // Informazioni admin
            HttpSession session = request.getSession(false);
            if (session != null) {
                String fullName = (String) session.getAttribute("full_name");
                dataModel.put("admin_name", fullName != null ? fullName : "Amministratore");
            }
            
            logger.info("Caricamento template admin_materiali.ftl.html");
            
            // Carica il template
            new webengineering.framework.result.TemplateResult(getServletContext())
                    .activate("admin_materiali.ftl.html", dataModel, response);
                    
            logger.info("Template caricato con successo");
                    
        } catch (TemplateManagerException ex) {
            logger.log(Level.SEVERE, "Errore nel template dei materiali", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nel caricamento della pagina");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        } catch (DataException ex) {
            logger.log(Level.SEVERE, "Errore nel caricamento dati disponibilità materiali", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nel caricamento dei dati");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        }
    }
    
    // Mostra i dettagli di un materiale specifico
    private void showDettagliMateriale(HttpServletRequest request, HttpServletResponse response, 
                                      int idMateriale, SoccorsoDataLayer dataLayer) throws ServletException {
        
        logger.info("=== showDettagliMateriale - ID: " + idMateriale + " ===");
        
        try {
            // Carica il materiale
            Materiale materiale = dataLayer.getMaterialeDAO().getMaterialeById(idMateriale);
            if (materiale == null) {
                logger.warning("Materiale non trovato con ID: " + idMateriale);
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Materiale non trovato");
                return;
            }
            
            // Carica storico missioni del materiale
            List<webengineering.nuovissimosoccorsoweb.model.Missione> storicoMissioni = 
                getMissioniByMateriale(idMateriale, dataLayer);
            
            logger.info("Trovate " + storicoMissioni.size() + " missioni per materiale " + idMateriale);
            
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
                    
                    // Crea il wrapper che gestisce automaticamente la formattazione data
                    missioniConDettagli.add(new MissioneConDettagli(missione, richiesta, infoMissione));
                } catch (DataException ex) {
                    logger.log(Level.WARNING, "Errore nel caricamento dettagli per missione " + missione.getCodiceRichiesta(), ex);
                    // Aggiungi la missione comunque, ma senza dettagli (saranno null)
                    missioniConDettagli.add(new MissioneConDettagli(missione, null, null));
                }
            }
            
            // Determina se è disponibile
            List<Materiale> materialiDisponibili = dataLayer.getMaterialeDAO().getMaterialiDisponibili();
            boolean disponibile = materialiDisponibili.stream()
                .anyMatch(matDisp -> matDisp.getId() == idMateriale);
            
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
            
            // Aggiungi dati materiale
            dataModel.put("materiale", materiale);
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
                    .activate("admin_materiale_dettagli.ftl.html", dataModel, response);
                    
        } catch (TemplateManagerException ex) {
            logger.log(Level.SEVERE, "Errore nel template dettagli materiale", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nel caricamento della pagina");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        } catch (DataException ex) {
            logger.log(Level.SEVERE, "Errore nel caricamento dettagli materiale", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nel caricamento dei dati");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico nei dettagli materiale", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore di sistema");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        }
    }
    
    // Metodo per ottenere tutte le missioni a cui ha partecipato un materiale
    private List<webengineering.nuovissimosoccorsoweb.model.Missione> getMissioniByMateriale(int idMateriale, SoccorsoDataLayer dataLayer) throws DataException {
        List<webengineering.nuovissimosoccorsoweb.model.Missione> missioni = new ArrayList<>();
        
        // Query per ottenere tutte le missioni che hanno utilizzato questo materiale
        try (PreparedStatement stmt = dataLayer.getConnection().prepareStatement(
                "SELECT m.* FROM missione m " +
                "JOIN utilizza_materiale um ON m.codice_richiesta = um.codice_missione " +
                "WHERE um.id_materiale = ? " +
                "ORDER BY m.data_ora_inizio DESC")) {
            
            stmt.setInt(1, idMateriale);
            
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
                    
                    // Gestione sicura della data - convertiamo in Date per Freemarker
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
            throw new DataException("Errore nel recupero delle missioni per materiale", e);
        }
        
        return missioni;
    }
    
    // Messaggi di successo
    private String getSuccessMessage(String successCode) {
        switch (successCode) {
            case "materiale_created":
                return "Materiale creato con successo.";
            case "materiale_updated":
                return "Materiale aggiornato con successo.";
            case "materiale_deleted":
                return "Materiale eliminato con successo.";
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
            case "materiale_not_found":
                return "Materiale non trovato.";
            case "validation_error":
                return "Dati non validi. Controlla i campi inseriti.";
            default:
                return "Si è verificato un errore.";
        }
    }
    
    // Classe di supporto per contenere missione + dettagli richiesta + info conclusione (per materiali)
    public static class MissioneConDettagli {
        private final webengineering.nuovissimosoccorsoweb.model.Missione missione;
        private final webengineering.nuovissimosoccorsoweb.model.RichiestaSoccorso richiesta;
        private final webengineering.nuovissimosoccorsoweb.model.InfoMissione infoMissione;
        private final String dataFormattata;
        
        public MissioneConDettagli(webengineering.nuovissimosoccorsoweb.model.Missione missione, 
                                  webengineering.nuovissimosoccorsoweb.model.RichiestaSoccorso richiesta, 
                                  webengineering.nuovissimosoccorsoweb.model.InfoMissione infoMissione) {
            this.missione = missione;
            this.richiesta = richiesta;
            this.infoMissione = infoMissione;
            
            // Converte LocalDateTime in stringa formattata per Freemarker
            String dataTemp;
            if (missione != null && missione.getDataOraInizio() != null) {
                try {
                    java.time.format.DateTimeFormatter formatter = 
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    dataTemp = missione.getDataOraInizio().format(formatter);
                } catch (Exception e) {
                    dataTemp = "Data non disponibile";
                }
            } else {
                dataTemp = "Data non disponibile";
            }
            this.dataFormattata = dataTemp;
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
        
        // Getter per la data già formattata come stringa
        public String getDataFormattata() {
            return dataFormattata;
        }
    }
}