package webengineering.nuovissimosoccorsoweb.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import webengineering.framework.result.TemplateManagerException;
import webengineering.framework.security.SecurityHelpers;
import webengineering.nuovissimosoccorsoweb.SoccorsoDataLayer;
import webengineering.framework.data.DataException;
import webengineering.nuovissimosoccorsoweb.model.Amministratore;
import webengineering.nuovissimosoccorsoweb.model.Operatore;

public class Login extends SoccorsoBaseController {
    
    private static final Logger logger = Logger.getLogger(Login.class.getName());
    
    // Mostra la pagina di login (GET)
    private void showLoginPage(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        try {
            // PRIMA VERIFICA: Se l'utente è già loggato, mostra pagina diversa
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute("userid") != null) {
                showAlreadyLoggedIn(request, response);
                return;
            }
            
            String passwordInChiaro = "Ciao"; // QUESTE TRE RIGHE SONO DI DEBUG
            String passwordHashata = SecurityHelpers.getPasswordHashPBKDF2(passwordInChiaro);
            System.out.println(passwordHashata);

            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("thispageurl", request.getAttribute("thispageurl"));
            dataModel.put("outline_tpl", null);
            
            // Aggiungi informazioni utente al dataModel
            addUserInfoToModel(request, dataModel);
            
            // Controlla se c'è una richiesta di HTTPS
            String httpsRedirect = SecurityHelpers.checkHttps(request);
            if (httpsRedirect != null) {
                dataModel.put("https_redirect", httpsRedirect);
            }
            
            // Messaggi di errore o successo
            String error = request.getParameter("error");
            String message = request.getParameter("message");
            String logout = request.getParameter("logout");
            
            if (error != null) {
                dataModel.put("error_message", getErrorMessage(error));
            }
            if (message != null) {
                dataModel.put("success_message", message);
            }
            if ("success".equals(logout)) {
                dataModel.put("logout_message", "Logout effettuato con successo. Arrivederci!");
            }
            
            // Referrer per redirect dopo login
            String referrer = request.getParameter("referrer");
            if (referrer != null && !referrer.trim().isEmpty()) {
                dataModel.put("referrer", referrer);
            }
            
            new webengineering.framework.result.TemplateResult(getServletContext())
                    .activate("login_full.ftl.html", dataModel, response);
                    
        } catch (TemplateManagerException ex) {
            logger.log(Level.SEVERE, "Errore nel template di login", ex);
            handleError(ex, request, response);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico nella pagina di login", ex);
            handleError(ex, request, response);
        }
    }
    
    // Mostra la pagina per utenti già loggati
    private void showAlreadyLoggedIn(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        try {
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("thispageurl", request.getAttribute("thispageurl"));
            dataModel.put("outline_tpl", null);
            
            // Aggiungi info utente al model
            addUserInfoToModel(request, dataModel);
            
            // Messaggio personalizzato
            String displayName = (String) dataModel.get("user_display_name");
            String roleDisplay = (String) dataModel.get("user_role_display");
            
            if (displayName != null && roleDisplay != null) {
                dataModel.put("already_logged_message", 
                    "Sei già loggato come " + displayName + " (" + roleDisplay + ")");
            } else {
                dataModel.put("already_logged_message", "Sei già loggato nel sistema!");
            }
            
            new webengineering.framework.result.TemplateResult(getServletContext())
                    .activate("already_logged_in.ftl.html", dataModel, response);
                    
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore nella visualizzazione pagina già loggato", ex);
            handleError(ex, request, response);
        }
    }
    
    // Gestisce l'autenticazione (POST)
    private void processLogin(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        try {
            // Recupera e sanifica i parametri dal form
            String email = request.getParameter("email");
            String password = request.getParameter("password");
            String referrer = request.getParameter("referrer");
            
            // Sanificazione input per prevenire SQL injection
            if (email != null) {
                email = SecurityHelpers.addSlashes(email.trim().toLowerCase());
            }
            
            // Validazione input
            if (email == null || email.isEmpty()) {
                redirectWithError(response, "email_required", referrer);
                return;
            }
            
            if (password == null || password.trim().isEmpty()) {
                redirectWithError(response, "password_required", referrer);
                return;
            }
            
            // Validazione formato email (basilare)
            if (!email.contains("@") || !email.contains(".")) {
                redirectWithError(response, "email_invalid", referrer);
                return;
            }
            
            // Ottieni il DataLayer
            SoccorsoDataLayer dataLayer = (SoccorsoDataLayer) request.getAttribute("datalayer");
            if (dataLayer == null) {
                logger.severe("DataLayer non disponibile nella request");
                redirectWithError(response, "system_error", referrer);
                return;
            }
            
            // Autenticazione
            UserInfo userInfo = authenticateUser(email, password, dataLayer);
            
            if (userInfo != null) {
                // Login riuscito: crea sessione sicura
                SecurityHelpers.createSession(request, userInfo.email, userInfo.userId);
                
                // Aggiungi informazioni aggiuntive alla sessione
                HttpSession session = request.getSession();
                session.setAttribute("user_type", userInfo.userType);
                session.setAttribute("user_role", userInfo.role);
                session.setAttribute("full_name", userInfo.fullName);
                
                // Log dell'accesso
                logger.info("Login riuscito per utente: " + userInfo.email + " (ID: " + userInfo.userId + 
                           ", Tipo: " + userInfo.userType + ", Ruolo: " + userInfo.role + 
                           ") da IP: " + request.getRemoteAddr());
                
                // Redirect appropriato
                if (referrer != null && !referrer.trim().isEmpty() && !referrer.contains("login")) {
                    response.sendRedirect(referrer);
                } else {
                    // Redirect diverso in base al ruolo
                    if ("admin".equals(userInfo.role)) {
                        response.sendRedirect(request.getContextPath() + "/admin/dashboard");
                    } else if ("operator".equals(userInfo.role)) {
                        response.sendRedirect(request.getContextPath() + "/operatore/dashboard");
                    } else {
                        // Fallback per ruoli non riconosciuti
                        response.sendRedirect(request.getContextPath() + "/");
                    }
                }
            } else {
                // Login fallito
                logger.warning("Tentativo di login fallito per email: " + email + " da IP: " + request.getRemoteAddr());
                redirectWithError(response, "invalid_credentials", referrer);
            }
            
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore durante il processo di login", ex);
            try {
                redirectWithError(response, "system_error", request.getParameter("referrer"));
            } catch (Exception e) {
                handleError(ex, request, response);
            }
        }
    }
    
    // Classe di supporto per le informazioni utente
    private static class UserInfo {
        String email;
        int userId;
        String userType; // "amministratore" o "operatore"
        String role; // "admin" o "operator"
        String fullName;
        
        UserInfo(String email, int userId, String userType, String role, String fullName) {
            this.email = email;
            this.userId = userId;
            this.userType = userType;
            this.role = role;
            this.fullName = fullName;
        }
    }
    
    // Autentica l'utente contro il database
    private UserInfo authenticateUser(String email, String password, SoccorsoDataLayer dataLayer) {
        try {
            // Rimuovi i backslash per la ricerca nel database
            String cleanEmail = SecurityHelpers.stripSlashes(email);
            
            // Prima cerca negli Amministratori
            try {
                
                Amministratore admin = dataLayer.getAmministratoreDAO().getAmministratoreByEmail(cleanEmail);
                if (admin != null) {
                    // Verifica password con PBKDF2
                    if (SecurityHelpers.checkPasswordHashPBKDF2(password, admin.getPassword())) {
                        String fullName = admin.getNome() + " " + admin.getCognome();
                        return new UserInfo(admin.getEmail(), admin.getId(), "amministratore", "admin", fullName);
                    }
                }
            } catch (DataException ex) {
                logger.log(Level.WARNING, "Errore nella ricerca amministratore: " + cleanEmail, ex);
            }
            
            // Poi cerca negli Operatori
            try {
                Operatore operatore = dataLayer.getOperatoreDAO().getOperatoreByEmail(cleanEmail);
                if (operatore != null) {
                    // Verifica password con PBKDF2
                    if (SecurityHelpers.checkPasswordHashPBKDF2(password, operatore.getPassword())) {
                        String fullName = operatore.getNome() + " " + operatore.getCognome();
                        return new UserInfo(operatore.getEmail(), operatore.getId(), "operatore", "operator", fullName);
                    }
                }
            } catch (DataException ex) {
                logger.log(Level.WARNING, "Errore nella ricerca operatore: " + cleanEmail, ex);
            }
            
            // DEMO temporaneo - rimuovi quando hai dati nel database
            if ("admin@soccorso.it".equals(cleanEmail) && "admin123".equals(password)) {
                return new UserInfo("admin@soccorso.it", 1, "amministratore", "admin", "Admin Demo");
            }
            if ("operatore@soccorso.it".equals(cleanEmail) && "op123".equals(password)) {
                return new UserInfo("operatore@soccorso.it", 2, "operatore", "operator", "Operatore Demo");
            }
            
            return null; // Utente non trovato o password errata
            
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            logger.log(Level.SEVERE, "Errore nell'algoritmo di hashing password", ex);
            return null;
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore durante l'autenticazione dell'utente: " + email, ex);
            return null;
        }
    }
    
    // Utility per redirect con errore
    private void redirectWithError(HttpServletResponse response, String errorCode, String referrer) 
            throws Exception {
        String redirectUrl = "login?error=" + errorCode;
        if (referrer != null && !referrer.trim().isEmpty()) {
            redirectUrl += "&referrer=" + java.net.URLEncoder.encode(referrer, "UTF-8");
        }
        response.sendRedirect(redirectUrl);
    }
    
    // Messaggi di errore localizzati
    private String getErrorMessage(String errorCode) {
        switch (errorCode) {
            case "email_required":
                return "L'indirizzo email è obbligatorio";
            case "password_required":
                return "La password è obbligatoria";
            case "email_invalid":
                return "L'indirizzo email non è valido";
            case "invalid_credentials":
                return "Email o password non corretti. Verifica i dati inseriti e riprova.";
            case "session_expired":
                return "La sessione è scaduta. Effettua nuovamente il login.";
            case "system_error":
                return "Si è verificato un errore di sistema. Riprova più tardi.";
            case "account_locked":
                return "Account temporaneamente bloccato per motivi di sicurezza";
            default:
                return "Si è verificato un errore durante il login";
        }
    }
    
    @Override
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        String method = request.getMethod();
        
        if ("POST".equals(method)) {
            // Gestisce il form di login
            processLogin(request, response);
        } else {
            // Mostra la pagina di login
            showLoginPage(request, response);
        }
    }
}