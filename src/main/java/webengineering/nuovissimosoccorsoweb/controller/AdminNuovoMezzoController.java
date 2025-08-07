package webengineering.nuovissimosoccorsoweb.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import webengineering.framework.result.TemplateManagerException;
import webengineering.nuovissimosoccorsoweb.SoccorsoDataLayer;
import webengineering.framework.data.DataException;
import webengineering.nuovissimosoccorsoweb.model.Mezzo;
import webengineering.nuovissimosoccorsoweb.model.impl.MezzoImpl;

public class AdminNuovoMezzoController extends SoccorsoBaseController {
    
    private static final Logger logger = Logger.getLogger(AdminNuovoMezzoController.class.getName());
    
    // Pattern per validazione targa italiana (formato: AB123CD o simili)
    private static final Pattern TARGA_PATTERN = Pattern.compile("^[A-Z]{2}\\d{3}[A-Z]{2}$");
    
    @Override
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        logger.info("=== AdminNuovoMezzoController - processRequest chiamato ===");
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
            logger.info("Mostrando form nuovo mezzo");
            showNuovoMezzoForm(request, response);
        } else if ("POST".equals(method)) {
            logger.info("Processando inserimento nuovo mezzo");
            processNuovoMezzo(request, response);
        } else {
            logger.warning("Metodo HTTP non supportato: " + method);
            try {
                response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            } catch (IOException ex) {
                Logger.getLogger(AdminNuovoMezzoController.class.getName()).log(Level.SEVERE, null, ex);
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
    
    // Mostra il form per inserire un nuovo mezzo
    private void showNuovoMezzoForm(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        logger.info("=== showNuovoMezzoForm ===");
        
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
            logger.info("Caricamento template admin_nuovo_mezzo.ftl.html");
            
            // Carica il template
            new webengineering.framework.result.TemplateResult(getServletContext())
                    .activate("admin_nuovo_mezzo.ftl.html", dataModel, response);
                    
            logger.info("Template caricato con successo");
                    
        } catch (TemplateManagerException ex) {
            logger.log(Level.SEVERE, "Errore nel template nuovo mezzo", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nel caricamento della pagina");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico in showNuovoMezzoForm", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore di sistema");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        }
    }
    
    // Processa l'inserimento di un nuovo mezzo
    private void processNuovoMezzo(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        logger.info("=== processNuovoMezzo ===");
        
        try {
            // Ottieni il DataLayer
            SoccorsoDataLayer dataLayer = (SoccorsoDataLayer) request.getAttribute("datalayer");
            if (dataLayer == null) {
                logger.severe("DataLayer non disponibile nella request");
                response.sendRedirect(request.getContextPath() + "/admin/NuovoMezzo?error=system_error");
                return;
            }
            
            // Ottieni i parametri dal form
            String targa = request.getParameter("targa");
            String nome = request.getParameter("nome");
            String descrizione = request.getParameter("descrizione");
            
            logger.info("Parametri ricevuti - Targa: " + targa + ", Nome: " + nome + ", Descrizione: " + descrizione);
            
            // Validazione dei dati
            ValidationResult validation = validateMezzoData(targa, nome, descrizione);
            if (!validation.isValid()) {
                logger.warning("Validazione fallita: " + validation.getErrorCode());
                response.sendRedirect(request.getContextPath() + "/admin/NuovoMezzo?error=" + validation.getErrorCode());
                return;
            }
            
            // Normalizza la targa (maiuscolo)
            targa = targa.trim().toUpperCase();
            logger.info("Targa normalizzata: " + targa);
            
            // Verifica che la targa non esista già
            Mezzo esistente = dataLayer.getMezzoDAO().getMezzoByTarga(targa);
            if (esistente != null) {
                logger.warning("Targa già esistente: " + targa);
                response.sendRedirect(request.getContextPath() + "/admin/NuovoMezzo?error=targa_exists");
                return;
            }
            
            // Crea il nuovo mezzo
            Mezzo nuovoMezzo = new MezzoImpl();
            nuovoMezzo.setTarga(targa);
            nuovoMezzo.setNome(nome.trim());
            nuovoMezzo.setDescrizione(descrizione.trim());
            nuovoMezzo.setVersion(1);
            
            logger.info("Creando nuovo mezzo: " + nuovoMezzo.getTarga());
            
            // Salva nel database
            dataLayer.getMezzoDAO().storeMezzo(nuovoMezzo);
            
            logger.info("Nuovo mezzo creato con successo con targa: " + targa);
            
            // Redirect alla lista mezzi con messaggio di successo
            response.sendRedirect(request.getContextPath() + "/admin/mezzi?success=mezzo_created");
            
        } catch (DataException ex) {
            logger.log(Level.SEVERE, "Errore database nella creazione mezzo", ex);
            try {
                response.sendRedirect(request.getContextPath() + "/admin/NuovoMezzo?error=database_error");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico nella creazione mezzo", ex);
            try {
                response.sendRedirect(request.getContextPath() + "/admin/NuovoMezzo?error=system_error");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        }
    }
    
    // Validazione dei dati del mezzo
    private ValidationResult validateMezzoData(String targa, String nome, String descrizione) {
        logger.info("=== validateMezzoData ===");
        
        // Validazione targa
        if (targa == null || targa.trim().isEmpty()) {
            logger.warning("Targa mancante");
            return new ValidationResult(false, "targa_required");
        }
        
        targa = targa.trim().toUpperCase();
        if (targa.length() != 7) {
            logger.warning("Lunghezza targa non valida: " + targa.length());
            return new ValidationResult(false, "targa_length");
        }
        
        if (!TARGA_PATTERN.matcher(targa).matches()) {
            logger.warning("Formato targa non valido: " + targa);
            return new ValidationResult(false, "targa_format");
        }
        
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
        
        if (descrizione.trim().length() > 45) {
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
            case "targa_required":
                return "La targa è obbligatoria.";
            case "targa_length":
                return "La targa deve essere di 7 caratteri (es: AB123CD).";
            case "targa_format":
                return "Formato targa non valido. Usa il formato AB123CD.";
            case "nome_required":
                return "Il nome del mezzo è obbligatorio.";
            case "nome_too_long":
                return "Il nome non può superare i 45 caratteri.";
            case "descrizione_required":
                return "La descrizione è obbligatoria.";
            case "descrizione_too_long":
                return "La descrizione non può superare i 45 caratteri.";
            case "targa_exists":
                return "Esiste già un mezzo con questa targa.";
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