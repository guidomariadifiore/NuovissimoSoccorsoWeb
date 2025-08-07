package webengineering.nuovissimosoccorsoweb.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import webengineering.framework.result.TemplateManagerException;
import webengineering.nuovissimosoccorsoweb.SoccorsoDataLayer;
import webengineering.framework.data.DataException;
import webengineering.nuovissimosoccorsoweb.model.Operatore;

public class AdminOperatoriController extends SoccorsoBaseController {
    
    private static final Logger logger = Logger.getLogger(AdminOperatoriController.class.getName());
    
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
            
            // Controlla se è una richiesta per dettagli operatore specifico
            String pathInfo = request.getPathInfo();
            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                // Estrai l'ID operatore dal path
                int operatoreId = Integer.parseInt(pathInfo.substring(1));
                showDettagliOperatore(request, response, operatoreId, dataLayer);
            } else {
                // Carica tutti gli operatori per la lista
                List<Operatore> operatori = loadAllOperatori(dataLayer);
                
                // Mostra la vista degli operatori
                showOperatoriView(request, response, operatori, dataLayer);
            }
            
        } catch (DataException ex) {
            logger.log(Level.SEVERE, "Errore database nel caricamento operatori", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nel caricamento dei dati");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        } catch (NumberFormatException ex) {
            logger.log(Level.WARNING, "ID operatore non valido nel path", ex);
            try {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID operatore non valido");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico nel caricamento operatori", ex);
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
    
    // Carica tutti gli operatori dal database
    private List<Operatore> loadAllOperatori(SoccorsoDataLayer dataLayer) throws DataException {
        return dataLayer.getOperatoreDAO().getAllOperatori();
    }
    
    // Determina se un operatore è disponibile o occupato (metodo rimosso - non più necessario)
    
    // Mostra i dettagli di un operatore specifico
    private void showDettagliOperatore(HttpServletRequest request, HttpServletResponse response, 
                                     int operatoreId, SoccorsoDataLayer dataLayer) throws ServletException {
        
        try {
            // Carica l'operatore
            Operatore operatore = dataLayer.getOperatoreDAO().getOperatoreById(operatoreId);
            if (operatore == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Operatore non trovato");
                return;
            }
            
            // Carica patenti dell'operatore
            List<webengineering.nuovissimosoccorsoweb.model.TipoPatente> patenti = 
                dataLayer.getOperatoreHaPatenteDAO().getPatentiByOperatore(operatoreId);
            
            // Carica abilità dell'operatore
            List<webengineering.nuovissimosoccorsoweb.model.Abilita> abilita = 
                dataLayer.getOperatoreHaAbilitaDAO().getAbilitaByOperatore(operatoreId);
            
            // Carica storico missioni dell'operatore
            List<webengineering.nuovissimosoccorsoweb.model.Missione> storicoMissioni = 
                dataLayer.getMissioneDAO().getMissioniByOperatore(operatoreId);
            
            logger.info("Trovate " + storicoMissioni.size() + " missioni per operatore " + operatoreId);
            
            // Per ogni missione, carica anche le informazioni della richiesta di soccorso, il ruolo e info conclusione
            List<MissioneConDettagli> missioniConDettagli = new ArrayList<>();
            for (webengineering.nuovissimosoccorsoweb.model.Missione missione : storicoMissioni) {
                try {
                    // Carica richiesta di soccorso collegata
                    webengineering.nuovissimosoccorsoweb.model.RichiestaSoccorso richiesta = 
                        dataLayer.getRichiestaSoccorsoDAO().getRichiestaByCodice(missione.getCodiceRichiesta());
                    
                    // Carica squadra per trovare il ruolo dell'operatore
                    List<webengineering.nuovissimosoccorsoweb.model.PartecipazioneSquadra> squadra = 
                        dataLayer.getMissioneDAO().getSquadraByMissione(missione.getCodiceRichiesta());
                    
                    webengineering.nuovissimosoccorsoweb.model.RuoloSquadra ruoloOperatore = null;
                    for (webengineering.nuovissimosoccorsoweb.model.PartecipazioneSquadra partecipazione : squadra) {
                        if (partecipazione.getIdOperatore() == operatoreId) {
                            ruoloOperatore = partecipazione.getRuolo();
                            break;
                        }
                    }
                    
                    // Carica info missione (data fine, successo, commento)
                    webengineering.nuovissimosoccorsoweb.model.InfoMissione infoMissione = null;
                    try {
                        infoMissione = dataLayer.getInfoMissioneDAO().getInfoByCodiceMissione(missione.getCodiceRichiesta());
                    } catch (DataException ex) {
                        logger.log(Level.WARNING, "InfoMissione non trovata per missione " + missione.getCodiceRichiesta(), ex);
                        // infoMissione rimane null se non trovata
                    }
                    
                    missioniConDettagli.add(new MissioneConDettagli(missione, richiesta, ruoloOperatore, infoMissione));
                } catch (DataException ex) {
                    logger.log(Level.WARNING, "Errore nel caricamento dettagli per missione " + missione.getCodiceRichiesta(), ex);
                    // Aggiungi la missione comunque, ma senza dettagli (saranno null)
                    missioniConDettagli.add(new MissioneConDettagli(missione, null, null, null));
                }
            }
            
            // Determina se è disponibile
            List<Operatore> operatoriDisponibili = dataLayer.getOperatoreDAO().getOperatoriDisponibili();
            boolean disponibile = operatoriDisponibili.stream()
                .anyMatch(op -> op.getId() == operatoreId);
            
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
            
            // Aggiungi dati operatore
            dataModel.put("operatore", operatore);
            dataModel.put("patenti", patenti);
            dataModel.put("abilita", abilita);
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
                    .activate("admin_operatore_dettagli.ftl.html", dataModel, response);
                    
        } catch (TemplateManagerException ex) {
            logger.log(Level.SEVERE, "Errore nel template dettagli operatore", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nel caricamento della pagina");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        } catch (DataException ex) {
            logger.log(Level.SEVERE, "Errore nel caricamento dettagli operatore", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nel caricamento dei dati");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico nei dettagli operatore", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore di sistema");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        }
    }
    
    // Mostra la vista con la lista degli operatori
    private void showOperatoriView(HttpServletRequest request, HttpServletResponse response, 
                                  List<Operatore> operatori, SoccorsoDataLayer dataLayer) throws ServletException {
        
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
            
            // Crea una mappa degli operatori con il loro stato di disponibilità
            Map<String, Boolean> operatoriDisponibilita = new HashMap<>();
            List<Operatore> operatoriDisponibili = dataLayer.getOperatoreDAO().getOperatoriDisponibili();
            
            for (Operatore op : operatori) {
                boolean disponibile = operatoriDisponibili.stream()
                    .anyMatch(opDisp -> opDisp.getId() == op.getId());
                operatoriDisponibilita.put(String.valueOf(op.getId()), disponibile);
            }
            
            // Aggiungi la lista degli operatori e la loro disponibilità
            dataModel.put("operatori", operatori);
            dataModel.put("operatori_disponibilita", operatoriDisponibilita);
            dataModel.put("total_operatori", operatori.size());
            
            // Calcola statistiche disponibilità
            long operatoriDisponibiliCount = operatoriDisponibilita.values().stream()
                .mapToLong(disponibile -> disponibile ? 1L : 0L).sum();
            long operatoriOccupatiCount = operatori.size() - operatoriDisponibiliCount;
            
            dataModel.put("operatori_disponibili_count", operatoriDisponibiliCount);
            dataModel.put("operatori_occupati_count", operatoriOccupatiCount);
            
            // Informazioni admin
            HttpSession session = request.getSession(false);
            if (session != null) {
                String fullName = (String) session.getAttribute("full_name");
                dataModel.put("admin_name", fullName != null ? fullName : "Amministratore");
            }
            
            // Carica il template
            new webengineering.framework.result.TemplateResult(getServletContext())
                    .activate("admin_operatori.ftl.html", dataModel, response);
                    
        } catch (TemplateManagerException ex) {
            logger.log(Level.SEVERE, "Errore nel template degli operatori", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nel caricamento della pagina");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        } catch (DataException ex) {
            logger.log(Level.SEVERE, "Errore nel caricamento dati disponibilità operatori", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nel caricamento dei dati");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        }
    }
    
    // Messaggi di successo
    private String getSuccessMessage(String successCode) {
        switch (successCode) {
            case "operatore_created":
                return "Operatore creato con successo.";
            case "operatore_updated":
                return "Operatore aggiornato con successo.";
            case "operatore_deleted":
                return "Operatore eliminato con successo.";
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
            case "operatore_not_found":
                return "Operatore non trovato.";
            case "validation_error":
                return "Dati non validi. Controlla i campi inseriti.";
            default:
                return "Si è verificato un errore.";
        }
    }
    
    // Classe di supporto per contenere missione + dettagli richiesta + ruolo operatore + info conclusione
    public static class MissioneConDettagli {
        private final webengineering.nuovissimosoccorsoweb.model.Missione missione;
        private final webengineering.nuovissimosoccorsoweb.model.RichiestaSoccorso richiesta;
        private final webengineering.nuovissimosoccorsoweb.model.RuoloSquadra ruolo;
        private final webengineering.nuovissimosoccorsoweb.model.InfoMissione infoMissione;
        
        public MissioneConDettagli(webengineering.nuovissimosoccorsoweb.model.Missione missione, 
                                  webengineering.nuovissimosoccorsoweb.model.RichiestaSoccorso richiesta, 
                                  webengineering.nuovissimosoccorsoweb.model.RuoloSquadra ruolo,
                                  webengineering.nuovissimosoccorsoweb.model.InfoMissione infoMissione) {
            this.missione = missione;
            this.richiesta = richiesta;
            this.ruolo = ruolo;
            this.infoMissione = infoMissione;
        }
        
        // Getter pubblici per Freemarker
        public webengineering.nuovissimosoccorsoweb.model.Missione getMissione() {
            return missione;
        }
        
        public webengineering.nuovissimosoccorsoweb.model.RichiestaSoccorso getRichiesta() {
            return richiesta;
        }
        
        public webengineering.nuovissimosoccorsoweb.model.RuoloSquadra getRuolo() {
            return ruolo;
        }
        
        public webengineering.nuovissimosoccorsoweb.model.InfoMissione getInfoMissione() {
            return infoMissione;
        }
    }
}