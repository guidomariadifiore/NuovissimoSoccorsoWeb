package webengineering.nuovissimosoccorsoweb.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import webengineering.framework.result.TemplateManagerException;
import webengineering.nuovissimosoccorsoweb.SoccorsoDataLayer;
import webengineering.framework.data.DataException;
import webengineering.nuovissimosoccorsoweb.model.RichiestaSoccorso;

public class VisualizzaEmail extends SoccorsoBaseController {
    
    private static final Logger logger = Logger.getLogger(VisualizzaEmail.class.getName());
    
    @Override
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        String token = request.getParameter("token");
        
        if (token == null || token.trim().isEmpty()) {
            // Token mancante - redirect alla home
            try {
                String contextPath = request.getContextPath();
                response.sendRedirect(contextPath + "/");
                return;
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Errore nel redirect", ex);
                handleError(ex, request, response);
                return;
            }
        }
        
        try {
            // Ottieni il DataLayer
            SoccorsoDataLayer dataLayer = (SoccorsoDataLayer) request.getAttribute("datalayer");
            if (dataLayer == null) {
                logger.severe("DataLayer non disponibile nella request");
                showError(request, response, "Errore di sistema. Riprova più tardi.");
                return;
            }
            
            // Cerca la richiesta per token
            RichiestaSoccorso richiesta = dataLayer.getRichiestaSoccorsoDAO().getRichiestaByStringaValidazione(token.trim());
            
            if (richiesta == null) {
                // Token non trovato
                logger.warning("Token non valido per visualizzazione email: " + token);
                showError(request, response, "Richiesta non trovata o token non valido.");
                return;
            }
            
            // Costruisci l'URL di conferma
            String confirmationUrl = buildConfirmationUrl(request, token);
            
            // Mostra la pagina email
            showEmailPage(request, response, richiesta, confirmationUrl);
            
        } catch (DataException ex) {
            logger.log(Level.SEVERE, "Errore database durante visualizzazione email", ex);
            showError(request, response, "Errore nel caricamento dei dati.");
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico durante visualizzazione email", ex);
            showError(request, response, "Errore di sistema.");
        }
    }
    
    // Costruisce l'URL di conferma
    private String buildConfirmationUrl(HttpServletRequest request, String token) {
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();
        
        return "http://" + serverName + 
               (serverPort != 80 ? ":" + serverPort : "") + 
               contextPath + "/conferma-richiesta?token=" + token;
    }
    
    // Mostra la pagina email
    private void showEmailPage(HttpServletRequest request, HttpServletResponse response, 
                              RichiestaSoccorso richiesta, String confirmationUrl) 
            throws ServletException {
        
        try {
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("thispageurl", request.getAttribute("thispageurl"));
            dataModel.put("outline_tpl", null);
            
            // Aggiungi informazioni utente al dataModel
            addUserInfoToModel(request, dataModel);
            
            // Dati della richiesta
            dataModel.put("richiesta", richiesta);
            dataModel.put("confirmation_url", confirmationUrl);
            
            // Dati specifici per l'email
            dataModel.put("tipo_emergenza", richiesta.getNome());
            dataModel.put("luogo", richiesta.getIndirizzo());
            dataModel.put("email_segnalante", richiesta.getEmailSegnalante());
            dataModel.put("nome_segnalante", richiesta.getNomeSegnalante());
            dataModel.put("descrizione", richiesta.getDescrizione());
            
            new webengineering.framework.result.TemplateResult(getServletContext())
                    .activate("email_conferma.ftl.html", dataModel, response);
                    
        } catch (TemplateManagerException ex) {
            logger.log(Level.SEVERE, "Errore nel template email", ex);
            handleError(ex, request, response);
        }
    }
    
    // Mostra pagina di errore
    private void showError(HttpServletRequest request, HttpServletResponse response, String message) 
            throws ServletException {
        
        try {
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("thispageurl", request.getAttribute("thispageurl"));
            dataModel.put("outline_tpl", null);
            
            addUserInfoToModel(request, dataModel);
            dataModel.put("error_message", message);
            
            new webengineering.framework.result.TemplateResult(getServletContext())
                    .activate("error.ftl.html", dataModel, response);
                    
        } catch (TemplateManagerException ex) {
            logger.log(Level.SEVERE, "Errore nel template di errore", ex);
            handleError(ex, request, response);
        }
    }
}