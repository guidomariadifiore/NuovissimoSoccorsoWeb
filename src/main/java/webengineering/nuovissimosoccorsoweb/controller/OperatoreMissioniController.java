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

public class OperatoreMissioniController extends SoccorsoBaseController {
    
    private static final Logger logger = Logger.getLogger(OperatoreMissioniController.class.getName());
    
    @Override
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        logger.info("=== OperatoreMissioniController - processRequest ===");
        
        // Verifica autenticazione operatore
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
            SoccorsoDataLayer dataLayer = (SoccorsoDataLayer) request.getAttribute("datalayer");
            if (dataLayer == null) {
                logger.severe("DataLayer non disponibile");
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore di sistema");
                return;
            }
            
            showMissioniInCorso(request, response, dataLayer);
            
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico nel controller missioni", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore di sistema");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        }
    }
    
    private boolean isUserOperatore(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        
        String userRole = (String) session.getAttribute("user_role");
        return "operatore".equals(userRole) || "operator".equals(userRole);
    }
    
    private void showMissioniInCorso(HttpServletRequest request, HttpServletResponse response, 
                                   SoccorsoDataLayer dataLayer) throws ServletException {
        
        logger.info("=== showMissioniInCorso ===");
        
        try {
            HttpSession session = request.getSession(false);
            if (session == null) {
                response.sendRedirect(request.getContextPath() + "/login");
                return;
            }
            
            // Ottieni ID operatore dalla sessione
            String operatoreIdStr = getOperatoreIdFromSession(session);
            if (operatoreIdStr == null) {
                logger.severe("ID operatore non trovato in sessione");
                response.sendRedirect(request.getContextPath() + "/operatore/dashboard?error=session_error");
                return;
            }
            
            int operatoreId = Integer.parseInt(operatoreIdStr);
            logger.info("Caricamento missioni in corso per operatore ID: " + operatoreId);
            
            // Carica missioni in corso usando il metodo ottimizzato del DAO
            List<Missione> missioniInCorso = dataLayer.getMissioneDAO().getMissioniInCorsoByOperatore(operatoreId);
            logger.info("Trovate " + missioniInCorso.size() + " missioni in corso per operatore " + operatoreId);
            
            // Prepara dati per il template
            Map<String, Object> dataModel = createDataModel(request, session, missioniInCorso, operatoreId);
            
            // Carica template
            new webengineering.framework.result.TemplateResult(getServletContext())
                    .activate("operatore_missioni_incorso.ftl.html", dataModel, response);
                    
            logger.info("Template caricato con successo");
                    
        } catch (NumberFormatException ex) {
            logger.log(Level.SEVERE, "Errore parsing ID operatore", ex);
            redirectWithError(response, request.getContextPath() + "/operatore/dashboard", "session_error");
        } catch (DataException ex) {
            logger.log(Level.SEVERE, "Errore database", ex);
            redirectWithError(response, request.getContextPath() + "/operatore/dashboard", "database_error");
        } catch (TemplateManagerException ex) {
            logger.log(Level.SEVERE, "Errore template", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore caricamento pagina");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico", ex);
            redirectWithError(response, request.getContextPath() + "/operatore/dashboard", "system_error");
        }
    }
    
    private String getOperatoreIdFromSession(HttpSession session) {
        // Prova diversi nomi di attributi
        String[] possibleNames = {"userid", "user_id", "id"};
        
        for (String name : possibleNames) {
            Object value = session.getAttribute(name);
            if (value != null) {
                logger.info("Trovato ID operatore con attributo '" + name + "': " + value + 
                           " (tipo: " + value.getClass().getSimpleName() + ")");
                
                // Gestisci sia Integer che String
                if (value instanceof Integer) {
                    return String.valueOf(value);
                } else if (value instanceof String) {
                    return (String) value;
                } else {
                    return value.toString();
                }
            }
        }
        
        // Debug completo sessione se non trovato
        logger.warning("ID operatore non trovato. Debug sessione:");
        java.util.Enumeration<String> attributeNames = session.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String attributeName = attributeNames.nextElement();
            Object attributeValue = session.getAttribute(attributeName);
            logger.info("Sessione - " + attributeName + ": " + attributeValue + 
                       " (tipo: " + (attributeValue != null ? attributeValue.getClass().getSimpleName() : "null") + ")");
        }
        
        return null;
    }
    
    private Map<String, Object> createDataModel(HttpServletRequest request, HttpSession session, 
                                               List<Missione> missioniInCorso, int operatoreId) {
        
        Map<String, Object> dataModel = new HashMap<>();
        
        // Base template data
        dataModel.put("thispageurl", request.getAttribute("thispageurl"));
        dataModel.put("outline_tpl", null);
        dataModel.put("contextPath", request.getContextPath());
        
        // User info
        addUserInfoToModel(request, dataModel);
        
        // Messaggi
        addMessages(request, dataModel);
        
        // Dati missioni
        dataModel.put("missioni_in_corso", missioniInCorso);
        dataModel.put("numero_missioni", missioniInCorso.size());
        
        // Informazioni operatore
        String fullName = (String) session.getAttribute("full_name");
        dataModel.put("operatore_name", fullName != null ? fullName : "Operatore");
        dataModel.put("operatore_id", operatoreId);
        
        return dataModel;
    }
    
    private void addMessages(HttpServletRequest request, Map<String, Object> dataModel) {
        String successParam = request.getParameter("success");
        String errorParam = request.getParameter("error");
        
        if (successParam != null && !successParam.trim().isEmpty()) {
            dataModel.put("success_message", getSuccessMessage(successParam));
        }
        
        if (errorParam != null && !errorParam.trim().isEmpty()) {
            dataModel.put("error_message", getErrorMessage(errorParam));
        }
    }
    
    private void redirectWithError(HttpServletResponse response, String url, String errorCode) {
        try {
            response.sendRedirect(url + "?error=" + errorCode);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore nel redirect con errore", e);
        }
    }
    
    private String getSuccessMessage(String successCode) {
        switch (successCode) {
            case "mission_updated":
                return "Missione aggiornata con successo.";
            default:
                return "Operazione completata con successo.";
        }
    }
    
    private String getErrorMessage(String errorCode) {
        switch (errorCode) {
            case "access_denied":
                return "Accesso negato. Solo gli operatori possono accedere.";
            case "session_error":
                return "Errore di sessione. ID operatore non trovato. Effettua nuovamente il login.";
            case "database_error":
                return "Errore nel database. Riprova più tardi.";
            case "system_error":
                return "Errore di sistema. Riprova più tardi.";
            default:
                return "Si è verificato un errore. Controlla i log per maggiori dettagli.";
        }
    }
}