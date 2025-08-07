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
import webengineering.nuovissimosoccorsoweb.model.Operatore;
import webengineering.nuovissimosoccorsoweb.model.TipoPatente;
import webengineering.nuovissimosoccorsoweb.model.Abilita;

public class OperatoreModificaController extends SoccorsoBaseController {
    
    private static final Logger logger = Logger.getLogger(OperatoreModificaController.class.getName());
    
    @Override
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        logger.info("=== OperatoreModificaController - processRequest ===");
        
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
        
        String method = request.getMethod();
        logger.info("Metodo richiesta: " + method);
        
        if ("GET".equals(method)) {
            showModificaForm(request, response);
        } else if ("POST".equals(method)) {
            processModifica(request, response);
        } else {
            logger.warning("Metodo HTTP non supportato: " + method);
            try {
                response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Errore critico", ex);
            }
        }
    }
    
    private boolean isUserOperatore(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        
        String userRole = (String) session.getAttribute("user_role");
        return "operatore".equals(userRole) || "operator".equals(userRole);
    }
    
    private void showModificaForm(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        logger.info("=== showModificaForm ===");
        
        try {
            SoccorsoDataLayer dataLayer = (SoccorsoDataLayer) request.getAttribute("datalayer");
            if (dataLayer == null) {
                logger.severe("DataLayer non disponibile");
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore di sistema");
                return;
            }
            
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
            logger.info("Caricamento dati per operatore ID: " + operatoreId);
            
            // Carica dati operatore
            Operatore operatore = dataLayer.getOperatoreDAO().getOperatoreById(operatoreId);
            if (operatore == null) {
                logger.severe("Operatore non trovato: " + operatoreId);
                response.sendRedirect(request.getContextPath() + "/operatore/dashboard?error=operatore_not_found");
                return;
            }
            
            // Carica patenti e abilità dell'operatore
            List<TipoPatente> patentiOperatore = dataLayer.getOperatoreHaPatenteDAO().getPatentiByOperatore(operatoreId);
            List<Abilita> abilitaOperatore = dataLayer.getOperatoreHaAbilitaDAO().getAbilitaByOperatore(operatoreId);
            
            // Carica tutte le patenti e abilità disponibili
            List<TipoPatente> tutteLePatenti = dataLayer.getPatentiDAO().getAllPatenti();
            List<Abilita> tutteLeAbilita = dataLayer.getAbilitaDAO().getAllAbilita();
            
            logger.info("Operatore: " + operatore.getNome() + " " + operatore.getCognome());
            logger.info("Patenti attuali: " + patentiOperatore.size());
            logger.info("Abilità attuali: " + abilitaOperatore.size());
            logger.info("Patenti disponibili: " + tutteLePatenti.size());
            logger.info("Abilità disponibili: " + tutteLeAbilita.size());
            
            // Prepara dati per il template
            Map<String, Object> dataModel = createDataModel(request, session, operatore, 
                patentiOperatore, abilitaOperatore, tutteLePatenti, tutteLeAbilita);
            
            // Carica template
            new webengineering.framework.result.TemplateResult(getServletContext())
                    .activate("operatore_modifica.ftl.html", dataModel, response);
                    
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
    
    private void processModifica(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        logger.info("=== processModifica ===");
        
        try {
            SoccorsoDataLayer dataLayer = (SoccorsoDataLayer) request.getAttribute("datalayer");
            if (dataLayer == null) {
                logger.severe("DataLayer non disponibile");
                redirectWithError(response, request.getContextPath() + "/operatore/modifica", "system_error");
                return;
            }
            
            HttpSession session = request.getSession(false);
            if (session == null) {
                response.sendRedirect(request.getContextPath() + "/login");
                return;
            }
            
            // Ottieni ID operatore dalla sessione
            String operatoreIdStr = getOperatoreIdFromSession(session);
            if (operatoreIdStr == null) {
                logger.severe("ID operatore non trovato in sessione");
                redirectWithError(response, request.getContextPath() + "/operatore/modifica", "session_error");
                return;
            }
            
            int operatoreId = Integer.parseInt(operatoreIdStr);
            logger.info("Aggiornamento dati per operatore ID: " + operatoreId);
            
            // Determina quale sezione è stata modificata
            String action = request.getParameter("action");
            logger.info("Azione richiesta: " + action);
            
            if ("update_patenti".equals(action)) {
                updatePatenti(operatoreId, request, dataLayer);
                response.sendRedirect(request.getContextPath() + "/operatore/modifica?success=patenti_updated");
            } else if ("update_abilita".equals(action)) {
                updateAbilita(operatoreId, request, dataLayer);
                response.sendRedirect(request.getContextPath() + "/operatore/modifica?success=abilita_updated");
            } else {
                logger.warning("Azione non riconosciuta: " + action);
                redirectWithError(response, request.getContextPath() + "/operatore/modifica", "invalid_action");
            }
            
        } catch (NumberFormatException ex) {
            logger.log(Level.SEVERE, "Errore parsing ID operatore", ex);
            redirectWithError(response, request.getContextPath() + "/operatore/modifica", "session_error");
        } catch (DataException ex) {
            logger.log(Level.SEVERE, "Errore database nel salvataggio", ex);
            redirectWithError(response, request.getContextPath() + "/operatore/modifica", "database_error");
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico nel salvataggio", ex);
            redirectWithError(response, request.getContextPath() + "/operatore/modifica", "system_error");
        }
    }
    
    private void updatePatenti(int operatoreId, HttpServletRequest request, SoccorsoDataLayer dataLayer) 
            throws DataException {
        
        logger.info("=== updatePatenti per operatore " + operatoreId + " ===");
        
        // Ottieni le patenti selezionate dal form
        String[] patentiSelezionate = request.getParameterValues("patenti");
        
        // Rimuovi tutte le patenti attuali
        dataLayer.getOperatoreHaPatenteDAO().rimuoviTutteLePatenti(operatoreId);
        logger.info("Rimosse tutte le patenti esistenti");
        
        // Aggiungi le nuove patenti selezionate
        if (patentiSelezionate != null && patentiSelezionate.length > 0) {
            for (String patenteStr : patentiSelezionate) {
                try {
                    TipoPatente tipoPatente = TipoPatente.fromString(patenteStr);
                    dataLayer.getOperatoreHaPatenteDAO().assegnaPatente(operatoreId, tipoPatente);
                    logger.info("Aggiunta patente: " + tipoPatente);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Errore nell'aggiunta patente: " + patenteStr, e);
                }
            }
            logger.info("Aggiunte " + patentiSelezionate.length + " nuove patenti");
        } else {
            logger.info("Nessuna patente selezionata");
        }
    }
    
    private void updateAbilita(int operatoreId, HttpServletRequest request, SoccorsoDataLayer dataLayer) 
            throws DataException {
        
        logger.info("=== updateAbilita per operatore " + operatoreId + " ===");
        
        // Ottieni le abilità selezionate dal form
        String[] abilitaSelezionate = request.getParameterValues("abilita");
        
        // Rimuovi tutte le abilità attuali
        dataLayer.getOperatoreHaAbilitaDAO().rimuoviTutteLeAbilita(operatoreId);
        logger.info("Rimosse tutte le abilità esistenti");
        
        // Aggiungi le nuove abilità selezionate
        if (abilitaSelezionate != null && abilitaSelezionate.length > 0) {
            for (String abilitaIdStr : abilitaSelezionate) {
                try {
                    int abilitaId = Integer.parseInt(abilitaIdStr);
                    dataLayer.getOperatoreHaAbilitaDAO().assegnaAbilita(operatoreId, abilitaId);
                    logger.info("Aggiunta abilità ID: " + abilitaId);
                } catch (NumberFormatException e) {
                    logger.log(Level.WARNING, "Errore nel parsing ID abilità: " + abilitaIdStr, e);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Errore nell'aggiunta abilità: " + abilitaIdStr, e);
                }
            }
            logger.info("Aggiunte " + abilitaSelezionate.length + " nuove abilità");
        } else {
            logger.info("Nessuna abilità selezionata");
        }
    }
    
    private String getOperatoreIdFromSession(HttpSession session) {
        String[] possibleNames = {"userid", "user_id", "id"};
        
        for (String name : possibleNames) {
            Object value = session.getAttribute(name);
            if (value != null) {
                if (value instanceof Integer) {
                    return String.valueOf(value);
                } else if (value instanceof String) {
                    return (String) value;
                } else {
                    return value.toString();
                }
            }
        }
        return null;
    }
    
    private Map<String, Object> createDataModel(HttpServletRequest request, HttpSession session, 
                                               Operatore operatore, List<TipoPatente> patentiOperatore,
                                               List<Abilita> abilitaOperatore, List<TipoPatente> tutteLePatenti,
                                               List<Abilita> tutteLeAbilita) {
        
        Map<String, Object> dataModel = new HashMap<>();
        
        // Base template data
        dataModel.put("thispageurl", request.getAttribute("thispageurl"));
        dataModel.put("outline_tpl", null);
        dataModel.put("contextPath", request.getContextPath());
        
        // User info
        addUserInfoToModel(request, dataModel);
        
        // Messaggi
        addMessages(request, dataModel);
        
        // Dati operatore
        dataModel.put("operatore", operatore);
        dataModel.put("patenti_operatore", patentiOperatore);
        dataModel.put("abilita_operatore", abilitaOperatore);
        dataModel.put("tutte_le_patenti", tutteLePatenti);
        dataModel.put("tutte_le_abilita", tutteLeAbilita);
        
        // Informazioni sessione
        String fullName = (String) session.getAttribute("full_name");
        dataModel.put("operatore_name", fullName != null ? fullName : "Operatore");
        
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
            case "patenti_updated":
                return "Patenti aggiornate con successo.";
            case "abilita_updated":
                return "Abilità aggiornate con successo.";
            default:
                return "Operazione completata con successo.";
        }
    }
    
    private String getErrorMessage(String errorCode) {
        switch (errorCode) {
            case "access_denied":
                return "Accesso negato. Solo gli operatori possono accedere.";
            case "session_error":
                return "Errore di sessione. Effettua nuovamente il login.";
            case "operatore_not_found":
                return "Operatore non trovato.";
            case "invalid_action":
                return "Azione non valida.";
            case "database_error":
                return "Errore nel database. Riprova più tardi.";
            case "system_error":
                return "Errore di sistema. Riprova più tardi.";
            default:
                return "Si è verificato un errore.";
        }
    }
}