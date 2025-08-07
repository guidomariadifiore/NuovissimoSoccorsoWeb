package webengineering.nuovissimosoccorsoweb.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import webengineering.framework.result.TemplateManagerException;
import webengineering.nuovissimosoccorsoweb.SoccorsoDataLayer;
import webengineering.framework.data.DataException;
import webengineering.nuovissimosoccorsoweb.model.Materiale;
import webengineering.nuovissimosoccorsoweb.model.impl.MaterialeImpl;

public class AdminNuovoMaterialeController extends SoccorsoBaseController {
    
    private static final Logger logger = Logger.getLogger(AdminNuovoMaterialeController.class.getName());
    
    @Override
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        logger.info("=== AdminNuovoMaterialeController - processRequest chiamato ===");
        logger.info("Metodo HTTP: " + request.getMethod());
        logger.info("Context Path: " + request.getContextPath());
        logger.info("Request URI: " + request.getRequestURI());
        logger.info("Query String: " + request.getQueryString());
        
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
        
        String method = request.getMethod();
        logger.info("Processando richiesta con metodo: " + method);
        
        if ("GET".equals(method)) {
            logger.info("Mostrando form nuovo materiale");
            showNuovoMaterialeForm(request, response);
        } else if ("POST".equals(method)) {
            logger.info("Processando inserimento nuovo materiale");
            processNuovoMateriale(request, response);
        } else {
            logger.warning("Metodo HTTP non supportato: " + method);
            try {
                response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            } catch (IOException ex) {
                Logger.getLogger(AdminNuovoMaterialeController.class.getName()).log(Level.SEVERE, null, ex);
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
    
    // Mostra il form per inserire un nuovo materiale
    private void showNuovoMaterialeForm(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        logger.info("=== showNuovoMaterialeForm ===");
        
        try {
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("thispageurl", request.getAttribute("thispageurl"));
            dataModel.put("outline_tpl", null);
            
            // Aggiungi informazioni utente
            addUserInfoToModel(request, dataModel);
            
            // Aggiungi context path
            dataModel.put("contextPath", request.getContextPath());
            
            // Gestisci messaggi di errore se presenti
            String errorParam = request.getParameter("error");
            if (errorParam != null && !errorParam.trim().isEmpty()) {
                String errorMessage = getErrorMessage(errorParam);
                logger.info("Aggiunto messaggio di errore: " + errorMessage);
                dataModel.put("error_message", errorMessage);
            }
            
            // Informazioni admin
            HttpSession session = request.getSession(false);
            if (session != null) {
                String fullName = (String) session.getAttribute("full_name");
                dataModel.put("admin_name", fullName != null ? fullName : "Amministratore");
                logger.info("Admin name: " + dataModel.get("admin_name"));
            }
            
            logger.info("Context path nel dataModel: " + dataModel.get("contextPath"));
            logger.info("Caricamento template admin_nuovo_materiale.ftl.html");
            
            // Carica il template
            new webengineering.framework.result.TemplateResult(getServletContext())
                    .activate("admin_nuovo_materiale.ftl.html", dataModel, response);
                    
            logger.info("Template caricato con successo");
                    
        } catch (TemplateManagerException ex) {
            logger.log(Level.SEVERE, "Errore nel template nuovo materiale", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nel caricamento della pagina");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico in showNuovoMaterialeForm", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore di sistema");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        }
    }
    
    // Processa l'inserimento di un nuovo materiale
    private void processNuovoMateriale(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        logger.info("=== processNuovoMateriale ===");
        
        try {
            // Ottieni il DataLayer
            SoccorsoDataLayer dataLayer = (SoccorsoDataLayer) request.getAttribute("datalayer");
            if (dataLayer == null) {
                logger.severe("DataLayer non disponibile nella request");
                response.sendRedirect(request.getContextPath() + "/admin/NuovoMateriale?error=system_error");
                return;
            }
            
            // Ottieni i parametri dal form
            String nome = request.getParameter("nome");
            String descrizione = request.getParameter("descrizione");
            
            logger.info("Parametri ricevuti - Nome: " + nome + ", Descrizione: " + descrizione);
            
            // Validazione dei dati
            ValidationResult validation = validateMaterialeData(nome, descrizione);
            if (!validation.isValid()) {
                logger.warning("Validazione fallita: " + validation.getErrorCode());
                response.sendRedirect(request.getContextPath() + "/admin/NuovoMateriale?error=" + validation.getErrorCode());
                return;
            }
                        
            // Crea il nuovo materiale
            Materiale nuovoMateriale = new MaterialeImpl();
            nuovoMateriale.setNome(nome.trim());
            nuovoMateriale.setDescrizione(descrizione.trim());
            nuovoMateriale.setVersion(1);
            
            logger.info("Creando nuovo materiale: " + nuovoMateriale.getNome());
            
            // Salva nel database
            dataLayer.getMaterialeDAO().storeMateriale(nuovoMateriale);
            
            logger.info("Nuovo materiale creato con successo: " + nome.trim());
            
            // Redirect alla lista materiali con messaggio di successo
            response.sendRedirect(request.getContextPath() + "/admin/materiali?success=materiale_created");
            
        } catch (DataException ex) {
            logger.log(Level.SEVERE, "Errore database nella creazione materiale", ex);
            try {
                response.sendRedirect(request.getContextPath() + "/admin/NuovoMateriale?error=database_error");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico nella creazione materiale", ex);
            try {
                response.sendRedirect(request.getContextPath() + "/admin/NuovoMateriale?error=system_error");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        }
    }
    
    // Validazione dei dati del materiale
    private ValidationResult validateMaterialeData(String nome, String descrizione) {
        logger.info("=== validateMaterialeData ===");
        
        // Validazione nome
        if (nome == null || nome.trim().isEmpty()) {
            logger.warning("Nome mancante");
            return new ValidationResult(false, "nome_required");
        }
        
        if (nome.trim().length() > 45) {
            logger.warning("Nome troppo lungo: " + nome.trim().length());
            return new ValidationResult(false, "nome_too_long");
        }
        
        // Validazione descrizione
        if (descrizione == null || descrizione.trim().isEmpty()) {
            logger.warning("Descrizione mancante");
            return new ValidationResult(false, "descrizione_required");
        }
        
        // Nota: la descrizione è di tipo TEXT nel DB, quindi non ha limiti di lunghezza stretti
        // ma manteniamo un controllo ragionevole per evitare contenuti eccessivamente lunghi
        if (descrizione.trim().length() > 1000) {
            logger.warning("Descrizione troppo lunga: " + descrizione.trim().length());
            return new ValidationResult(false, "descrizione_too_long");
        }
        
        logger.info("Validazione completata con successo");
        return new ValidationResult(true, null);
    }
    
    // Messaggi di errore
    private String getErrorMessage(String errorCode) {
        switch (errorCode) {
            case "access_denied":
                return "Accesso negato. Solo gli amministratori possono accedere.";
            case "nome_required":
                return "Il nome del materiale è obbligatorio.";
            case "nome_too_long":
                return "Il nome non può superare i 45 caratteri.";
            case "nome_exists":
                return "Esiste già un materiale con questo nome.";
            case "descrizione_required":
                return "La descrizione è obbligatoria.";
            case "descrizione_too_long":
                return "La descrizione non può superare i 1000 caratteri.";
            case "database_error":
                return "Errore nel database. Riprova più tardi.";
            case "system_error":
                return "Errore di sistema. Riprova più tardi.";
            default:
                return "Si è verificato un errore.";
        }
    }
    
    // Classe helper per la validazione
    private static class ValidationResult {
        private final boolean valid;
        private final String errorCode;
        
        public ValidationResult(boolean valid, String errorCode) {
            this.valid = valid;
            this.errorCode = errorCode;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorCode() {
            return errorCode;
        }
    }
}