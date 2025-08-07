package webengineering.nuovissimosoccorsoweb.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import webengineering.framework.result.TemplateManagerException;
import webengineering.nuovissimosoccorsoweb.SoccorsoDataLayer;
import webengineering.framework.data.DataException;
import webengineering.nuovissimosoccorsoweb.model.Missione;
import webengineering.nuovissimosoccorsoweb.model.RichiestaSoccorso;

public class OperatoreDashboardController extends SoccorsoBaseController {
    
    private static final Logger logger = Logger.getLogger(OperatoreDashboardController.class.getName());
    
    @Override
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        logger.info("=== OperatoreDashboardController - processRequest chiamato ===");
        
        // Verifica che l'utente sia un operatore (il filtro AuthenticationFilter già lo gestisce,
        // ma aggiungiamo controllo extra per sicurezza)
        if (!isUserOperatore(request)) {
            logger.warning("Accesso negato - utente non operatore");
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
            
            // Mostra la dashboard operatore
            showOperatoreDashboard(request, response, dataLayer);
            
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico nella dashboard operatore", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore di sistema");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        }
    }
    
    // Verifica se l'utente è un operatore (usando la stessa logica dell'admin)
    private boolean isUserOperatore(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        
        String userRole = (String) session.getAttribute("user_role");
        return "operatore".equals(userRole) || "operator".equals(userRole);
    }
    
    // Mostra la dashboard operatore
    private void showOperatoreDashboard(HttpServletRequest request, HttpServletResponse response, 
                                      SoccorsoDataLayer dataLayer) throws ServletException {
        
        logger.info("=== showOperatoreDashboard ===");
        
        try {
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("thispageurl", request.getAttribute("thispageurl"));
            dataModel.put("outline_tpl", null);
            
            // Aggiungi informazioni utente
            addUserInfoToModel(request, dataModel);
            
            // Aggiungi context path
            dataModel.put("contextPath", request.getContextPath());
            
            // Gestisci messaggi di successo ed errore
            String successParam = request.getParameter("success");
            String errorParam = request.getParameter("error");
            
            if (successParam != null && !successParam.trim().isEmpty()) {
                dataModel.put("success_message", getSuccessMessage(successParam));
            }
            
            if (errorParam != null && !errorParam.trim().isEmpty()) {
                dataModel.put("error_message", getErrorMessage(errorParam));
            }
            
            // Informazioni operatore
            HttpSession session = request.getSession(false);
            if (session != null) {
                String fullName = (String) session.getAttribute("full_name");
                Object operatoreIdObj = session.getAttribute("userid");
                String operatoreId = null;
                
                // Gestisci sia Integer che String per l'ID
                if (operatoreIdObj instanceof Integer) {
                    operatoreId = String.valueOf(operatoreIdObj);
                } else if (operatoreIdObj instanceof String) {
                    operatoreId = (String) operatoreIdObj;
                }
                
                dataModel.put("operatore_name", fullName != null ? fullName : "Operatore");
                dataModel.put("operatore_id", operatoreId);
                logger.info("Operatore name: " + dataModel.get("operatore_name"));
                logger.info("Operatore ID: " + operatoreId);
            }
            
            // Carica statistiche per la dashboard
            loadDashboardStats(dataLayer, dataModel, session);
            
            logger.info("Context path nel dataModel: " + dataModel.get("contextPath"));
            logger.info("Caricamento template operatore_dashboard.ftl.html");
            
            // Carica il template
            new webengineering.framework.result.TemplateResult(getServletContext())
                    .activate("operatore_dashboard.ftl.html", dataModel, response);
                    
            logger.info("Template caricato con successo");
                    
        } catch (TemplateManagerException ex) {
            logger.log(Level.SEVERE, "Errore nel template dashboard operatore", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nel caricamento della pagina");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico in showOperatoreDashboard", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore di sistema");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        }
    }
    
    // Carica le statistiche per la dashboard operatore
    private void loadDashboardStats(SoccorsoDataLayer dataLayer, Map<String, Object> dataModel, HttpSession session) {
        logger.info("=== loadDashboardStats ===");
        
        try {
            Object operatoreIdObj = session.getAttribute("userid");
            String operatoreId = null;
            
            // Gestisci sia Integer che String per l'ID
            if (operatoreIdObj instanceof Integer) {
                operatoreId = String.valueOf(operatoreIdObj);
            } else if (operatoreIdObj instanceof String) {
                operatoreId = (String) operatoreIdObj;
            }
            
            if (operatoreId == null) {
                logger.warning("ID operatore non disponibile in sessione (userid)");
                // Imposta valori di default
                dataModel.put("missioni_in_corso", 0);
                dataModel.put("storico_missioni", 0);
                dataModel.put("patenti_aggiornate", "true");
                dataModel.put("patenti_status", "aggiornate");
                return;
            }
            
            // TODO: Implementare quando avremo i DAO necessari
            // Per ora impostiamo valori di default per evitare errori nel template
            
            // Missioni in corso - usa il metodo ottimizzato del DAO
            int missioniInCorso = 0;
            try {
                int operatoreIdInt = Integer.parseInt(operatoreId);
                missioniInCorso = dataLayer.getMissioneDAO().countMissioniInCorsoByOperatore(operatoreIdInt);
                logger.info("Trovate " + missioniInCorso + " missioni in corso per operatore " + operatoreId);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Errore nel caricamento missioni in corso", e);
                missioniInCorso = 0;
            }
            
            dataModel.put("missioni_in_corso", missioniInCorso);
            
            // Storico missioni - usa il metodo ottimizzato del DAO
            int storicoMissioni = 0;
            try {
                int operatoreIdInt = Integer.parseInt(operatoreId);
                storicoMissioni = dataLayer.getMissioneDAO().countMissioniCompletateByOperatore(operatoreIdInt);
                logger.info("Trovate " + storicoMissioni + " missioni completate per operatore " + operatoreId);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Errore nel caricamento storico missioni", e);
                storicoMissioni = 0;
            }
            
            dataModel.put("storico_missioni", storicoMissioni);
            
            // Stato patenti/abilità - TODO: dataLayer.getOperatoreDAO().hasPatentiAggiornate(operatoreId);
            boolean patentiAggiornate = true;
            dataModel.put("patenti_aggiornate", patentiAggiornate ? "true" : "false");
            dataModel.put("patenti_status", patentiAggiornate ? "aggiornate" : "da_aggiornare");
            
            // TODO: Quando implementato, aggiungere:
            // List<Missione> ultimeMissioni = dataLayer.getMissioneDAO().getUltimeMissioniByOperatore(operatoreId, 5);
            // dataModel.put("ultime_missioni", ultimeMissioni);
            
            logger.info("Statistiche caricate - Missioni in corso: " + missioniInCorso + 
                       ", Storico: " + storicoMissioni + 
                       ", Patenti aggiornate: " + patentiAggiornate);
                       
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Errore nel caricamento delle statistiche operatore", ex);
            // Imposta valori di default in caso di errore
            dataModel.put("missioni_in_corso", 0);
            dataModel.put("storico_missioni", 0);
            dataModel.put("patenti_aggiornate", "true");
            dataModel.put("patenti_status", "aggiornate");
        }
    }
    
    // Messaggi di successo
    private String getSuccessMessage(String successCode) {
        switch (successCode) {
            case "profile_updated":
                return "Profilo aggiornato con successo.";
            case "mission_completed":
                return "Missione completata con successo.";
            default:
                return "Operazione completata con successo.";
        }
    }
    
    // Messaggi di errore
    private String getErrorMessage(String errorCode) {
        switch (errorCode) {
            case "access_denied":
                return "Accesso negato. Solo gli operatori possono accedere.";
            case "database_error":
                return "Errore nel database. Riprova più tardi.";
            case "system_error":
                return "Errore di sistema. Riprova più tardi.";
            default:
                return "Si è verificato un errore.";
        }
    }
}