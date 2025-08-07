package webengineering.nuovissimosoccorsoweb.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import webengineering.framework.result.TemplateManagerException;
import webengineering.framework.security.SecurityHelpers;
import webengineering.nuovissimosoccorsoweb.SoccorsoDataLayer;
import webengineering.framework.data.DataException;
import webengineering.nuovissimosoccorsoweb.model.RichiestaSoccorso;
import webengineering.nuovissimosoccorsoweb.model.impl.RichiestaSoccorsoImpl;
import webengineering.nuovissimosoccorsoweb.util.RateLimiter;

public class Home extends SoccorsoBaseController {
    
    private static final Logger logger = Logger.getLogger(Home.class.getName());
    private final RateLimiter rateLimiter = RateLimiter.getInstance();
    
    // Mostra il form di segnalazione (GET)
    private void showEmergencyForm(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        try {
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("thispageurl", request.getAttribute("thispageurl"));
            dataModel.put("outline_tpl", null);
            
            // Aggiungi informazioni utente al dataModel
            addUserInfoToModel(request, dataModel);
            
            // Messaggi di errore o successo
            String error = request.getParameter("error");
            String success = request.getParameter("success");
            String emailPreview = request.getParameter("email");
            
            if (error != null) {
                String errorMessage = getErrorMessage(error);
                
                // Se l'errore è rate limiting, aggiungi info sul tempo rimanente
                if ("rate_limit_ip".equals(error)) {
                    String clientIP = getClientIP(request);
                    long remainingMinutes = rateLimiter.getRemainingTimeMinutes(clientIP);
                    errorMessage += " Riprova tra " + remainingMinutes + " minuti.";
                } else if ("rate_limit_email".equals(error)) {
                    String email = request.getParameter("blocked_email");
                    if (email != null) {
                        long remainingMinutes = rateLimiter.getEmailRemainingTimeMinutes(email);
                        errorMessage += " Riprova tra " + remainingMinutes + " minuti.";
                    }
                } else if ("rate_limit_both".equals(error)) {
                    String clientIP = getClientIP(request);
                    String email = request.getParameter("blocked_email");
                    long ipRemainingMinutes = rateLimiter.getRemainingTimeMinutes(clientIP);
                    long emailRemainingMinutes = rateLimiter.getEmailRemainingTimeMinutes(email);
                    long maxRemaining = Math.max(ipRemainingMinutes, emailRemainingMinutes);
                    errorMessage += " Riprova tra " + maxRemaining + " minuti.";
                }
                
                dataModel.put("error_message", errorMessage);
            }
            
            if (success != null) {
                dataModel.put("success_message", "Richiesta di soccorso inviata con successo! Codice emergenza: #" + success);
            }
            
            // Mostra anteprima email se richiesta
            if ("true".equals(emailPreview)) {
                String token = request.getParameter("token");
                String email = request.getParameter("recipient");
                String nome = request.getParameter("nome_emergenza");
                String indirizzo = request.getParameter("indirizzo_emergenza");
                
                if (token != null && email != null) {
                    String emailContent = generateEmailContent(token, email, nome, indirizzo, request);
                    dataModel.put("email_preview", emailContent);
                    dataModel.put("email_subject", "Conferma la tua richiesta di soccorso");
                    dataModel.put("email_recipient", email);
                }
            }
            
            new webengineering.framework.result.TemplateResult(getServletContext())
                    .activate("emergency_form.ftl.html", dataModel, response);
                    
        } catch (TemplateManagerException ex) {
            logger.log(Level.SEVERE, "Errore nel template del form emergenza", ex);
            handleError(ex, request, response);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico nel form emergenza", ex);
            handleError(ex, request, response);
        }
    }
    
    // Gestisce l'invio della richiesta (POST)
    private void processEmergencyRequest(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, Exception {
        
        try {
            // Recupera e sanifica i parametri dal form
            String indirizzo = request.getParameter("indirizzo");
            String descrizione = request.getParameter("descrizione");
            String nomeEmergenza = request.getParameter("nome");
            String emailSegnalante = request.getParameter("email_segnalante");
            String nomeSegnalante = request.getParameter("nome_segnalante");
            String coordinate = request.getParameter("coordinate");
            String captchaCheck = request.getParameter("captcha_check");
            
            // CONTROLLO CAPTCHA
            if (captchaCheck == null || !"confirmed".equals(captchaCheck)) {
                logger.warning("Tentativo di invio senza conferma captcha");
                redirectWithError(response, "captcha_required", request, null);
                return;
            }
            
            // Sanificazione per SQL injection
            if (indirizzo != null) indirizzo = SecurityHelpers.addSlashes(indirizzo.trim());
            if (descrizione != null) descrizione = SecurityHelpers.addSlashes(descrizione.trim());
            if (nomeEmergenza != null) nomeEmergenza = SecurityHelpers.addSlashes(nomeEmergenza.trim());
            if (emailSegnalante != null) emailSegnalante = SecurityHelpers.addSlashes(emailSegnalante.trim().toLowerCase());
            if (nomeSegnalante != null) nomeSegnalante = SecurityHelpers.addSlashes(nomeSegnalante.trim());
            
            // Validazione input
            if (indirizzo == null || indirizzo.isEmpty()) {
                redirectWithError(response, "indirizzo_required", request, emailSegnalante);
                return;
            }
            
            if (descrizione == null || descrizione.isEmpty()) {
                redirectWithError(response, "descrizione_required", request, emailSegnalante);
                return;
            }
            
            if (nomeEmergenza == null || nomeEmergenza.isEmpty()) {
                redirectWithError(response, "nome_required", request, emailSegnalante);
                return;
            }
            
            if (emailSegnalante == null || emailSegnalante.isEmpty()) {
                redirectWithError(response, "email_required", request, null);
                return;
            }
            
            if (nomeSegnalante == null || nomeSegnalante.isEmpty()) {
                redirectWithError(response, "nome_segnalante_required", request, emailSegnalante);
                return;
            }
            
            // Validazione email basilare
            if (!emailSegnalante.contains("@") || !emailSegnalante.contains(".")) {
                redirectWithError(response, "email_invalid", request, emailSegnalante);
                return;
            }
            
            // CONTROLLO RATE LIMITING PER IP E EMAIL
            String clientIP = getClientIP(request);
            boolean ipAllowed = rateLimiter.isIPAllowed(clientIP);
            boolean emailAllowed = rateLimiter.isEmailAllowed(emailSegnalante);
            
            if (!ipAllowed && !emailAllowed) {
                logger.warning("Richiesta rifiutata per rate limiting da IP: " + clientIP + " e email: " + emailSegnalante);
                redirectWithError(response, "rate_limit_both", request, emailSegnalante);
                return;
            } else if (!ipAllowed) {
                logger.warning("Richiesta rifiutata per rate limiting da IP: " + clientIP);
                redirectWithError(response, "rate_limit_ip", request, emailSegnalante);
                return;
            } else if (!emailAllowed) {
                logger.warning("Richiesta rifiutata per rate limiting da email: " + emailSegnalante);
                redirectWithError(response, "rate_limit_email", request, emailSegnalante);
                return;
            }
            
            // Ottieni il DataLayer
            SoccorsoDataLayer dataLayer = (SoccorsoDataLayer) request.getAttribute("datalayer");
            if (dataLayer == null) {
                logger.severe("DataLayer non disponibile nella request");
                redirectWithError(response, "system_error", request, emailSegnalante);
                return;
            }
            
            // Verifica DAO
            if (dataLayer.getRichiestaSoccorsoDAO() == null) {
                logger.severe("DAO non disponibile");
                redirectWithError(response, "system_error", request, emailSegnalante);
                return;
            }
            
            // Genera token di conferma sicuro
            String confirmationToken = generateSecureValidationToken();
            
            // Crea l'oggetto richiesta
            RichiestaSoccorso richiesta = new RichiestaSoccorsoImpl();
            
            // Campi inseriti dall'utente
            richiesta.setIndirizzo(SecurityHelpers.stripSlashes(indirizzo));
            richiesta.setDescrizione(SecurityHelpers.stripSlashes(descrizione));
            richiesta.setNome(SecurityHelpers.stripSlashes(nomeEmergenza));
            richiesta.setEmailSegnalante(SecurityHelpers.stripSlashes(emailSegnalante));
            richiesta.setNomeSegnalante(SecurityHelpers.stripSlashes(nomeSegnalante));
            
            // Coordinate opzionali
            if (coordinate != null && !coordinate.trim().isEmpty()) {
                richiesta.setCoordinate(coordinate.trim());
            }
            
            // Campi gestiti dal sistema
            richiesta.setStato("Inviata");
            richiesta.setIp(clientIP);
            richiesta.setStringa(confirmationToken);
            richiesta.setFoto(null);
            
            // Salva nel database
            dataLayer.getRichiestaSoccorsoDAO().storeRichiesta(richiesta);
            
            // Log dell'operazione
            logger.info("Nuova richiesta di soccorso creata: " + 
                       "Indirizzo=" + richiesta.getIndirizzo() + 
                       ", Segnalante=" + richiesta.getNomeSegnalante() + 
                       ", Email=" + richiesta.getEmailSegnalante() +
                       ", Token=" + confirmationToken + 
                       ", IP=" + richiesta.getIp());
            
            // Redirect alla pagina email
            String contextPath = request.getContextPath();
            String successUrl = contextPath + "/emergenza?success=" + confirmationToken;
            response.sendRedirect(successUrl);            
        } catch (DataException ex) {
            logger.log(Level.SEVERE, "Errore database durante creazione richiesta", ex);
            redirectWithError(response, "database_error", request, null);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico durante creazione richiesta", ex);
            redirectWithError(response, "system_error", request, null);
        }
    }
    
    // Genera un token di conferma sicuro (64 caratteri)
    private String generateSecureValidationToken() {
        SecureRandom random = new SecureRandom();
        StringBuilder token = new StringBuilder(64);
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz";
        
        for (int i = 0; i < 64; i++) {
            token.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return token.toString();
    }
    
    // Genera il contenuto dell'email di conferma
    private String generateEmailContent(String token, String email, String nomeEmergenza, 
                                      String indirizzo, HttpServletRequest request) {
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();
        
        // Costruisci l'URL di conferma
        String confirmationUrl = "http://" + serverName + 
                                (serverPort != 80 ? ":" + serverPort : "") + 
                                contextPath + "/conferma-richiesta?token=" + token;
        
        // Email semplice e pulita
        return "Oggetto: Conferma la tua richiesta di soccorso\n\n" +
               "Ciao,\n\n" +
               "Hai inviato una richiesta di soccorso per: " + (nomeEmergenza != null ? nomeEmergenza : "Emergenza") + "\n" +
               "Luogo: " + (indirizzo != null ? indirizzo : "Non specificato") + "\n\n" +
               "Per attivare la richiesta, clicca su questo link:\n" +
               confirmationUrl + "\n\n" +
               "Se non hai fatto tu questa richiesta, ignora questa email.\n\n" +
               "Sistema di Soccorso";
    }
    
    // Ottiene l'IP reale del client (gestisce proxy/load balancer)
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }
    
    // Utility per redirect con errore (aggiornata per gestire l'email bloccata)
    private void redirectWithError(HttpServletResponse response, String errorCode, 
                                 HttpServletRequest request, String blockedEmail) throws Exception {
        String contextPath = request.getContextPath();
        String errorUrl = contextPath + "/emergenza?error=" + errorCode;
        
        // Aggiungi l'email bloccata se presente (per mostrare il tempo rimanente)
        if (blockedEmail != null && !blockedEmail.isEmpty()) {
            errorUrl += "&blocked_email=" + java.net.URLEncoder.encode(blockedEmail, "UTF-8");
        }
        
        response.sendRedirect(errorUrl);
    }
    
    // Messaggi di errore localizzati (aggiornati)
    private String getErrorMessage(String errorCode) {
        switch (errorCode) {
            case "indirizzo_required":
                return "L'indirizzo è obbligatorio";
            case "descrizione_required":
                return "La descrizione dell'emergenza è obbligatoria";
            case "nome_required":
                return "Il nome dell'emergenza è obbligatorio";
            case "email_required":
                return "L'email del segnalante è obbligatoria";
            case "nome_segnalante_required":
                return "Il nome del segnalante è obbligatorio";
            case "email_invalid":
                return "L'indirizzo email non è valido";
            case "captcha_required":
                return "Devi confermare di non essere un robot";
            case "rate_limit_ip":
                return "Il tuo indirizzo IP ha già inviato una richiesta di emergenza nell'ultima ora.";
            case "rate_limit_email":
                return "Questa email ha già inviato una richiesta di emergenza nell'ultima ora.";
            case "rate_limit_both":
                return "Sia il tuo IP che questa email hanno già inviato una richiesta nell'ultima ora.";
            case "database_error":
                return "Errore nel salvataggio dei dati. Riprova più tardi.";
            case "system_error":
                return "Si è verificato un errore di sistema. Riprova più tardi.";
            default:
                return "Si è verificato un errore durante l'invio della richiesta";
        }
    }
    
    @Override
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        String method = request.getMethod();
        
        if ("POST".equals(method)) {
            try {
                processEmergencyRequest(request, response);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Errore durante processEmergencyRequest", ex);
                showEmergencyForm(request, response);
            }
        } else {
            showEmergencyForm(request, response);
        }
    }
    
    @Override
    public void destroy() {
        // Shutdown del rate limiter quando la servlet viene distrutta
        rateLimiter.shutdown();
        super.destroy();
    }
}