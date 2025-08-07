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

public class RichiestaDetailsController extends SoccorsoBaseController {
    
    private static final Logger logger = Logger.getLogger(RichiestaDetailsController.class.getName());
    
    @Override
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        // Verifica amministratore
        if (!isUserAdmin(request)) {
            try {
                response.sendRedirect(request.getContextPath() + "/login?error=access_denied");
                return;
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Errore redirect accesso negato", ex);
                handleError(ex, request, response);
                return;
            }
        }
        
        String method = request.getMethod();
        
        if ("POST".equals(method)) {
            // Gestisce azioni sui bottoni
            handleAction(request, response);
        } else {
            // Mostra dettagli richiesta
            showDetails(request, response);
        }
    }
    
    private boolean isUserAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        
        String userRole = (String) session.getAttribute("user_role");
        return "admin".equals(userRole);
    }
    
    private void showDetails(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        try {
            // Estrai ID dalla URL
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || pathInfo.length() <= 1) {
                response.sendRedirect(request.getContextPath() + "/admin/dashboard");
                return;
            }
            
            String idString = pathInfo.substring(1);
            int richiestaId;
            
            try {
                richiestaId = Integer.parseInt(idString);
            } catch (NumberFormatException ex) {
                logger.warning("ID non valido: " + idString);
                response.sendRedirect(request.getContextPath() + "/admin/dashboard?error=invalid_id");
                return;
            }
            
            // Ottieni DataLayer
            SoccorsoDataLayer dataLayer = (SoccorsoDataLayer) request.getAttribute("datalayer");
            if (dataLayer == null) {
                logger.severe("DataLayer non disponibile");
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore sistema");
                return;
            }
            
            // Carica richiesta
            RichiestaSoccorso richiesta = dataLayer.getRichiestaSoccorsoDAO().getRichiestaByCodice(richiestaId);
            
            if (richiesta == null) {
                logger.warning("Richiesta non trovata: " + richiestaId);
                response.sendRedirect(request.getContextPath() + "/admin/dashboard?error=not_found");
                return;
            }
            
            // Mostra pagina
            displayDetailsPage(request, response, richiesta);
            
        } catch (DataException ex) {
            logger.log(Level.SEVERE, "Errore database", ex);
            showErrorPage(request, response, "Errore caricamento dati");
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico", ex);
            showErrorPage(request, response, "Errore di sistema");
        }
    }
    
    private void displayDetailsPage(HttpServletRequest request, HttpServletResponse response, 
                                   RichiestaSoccorso richiesta) throws ServletException {
        
        try {
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("thispageurl", request.getAttribute("thispageurl"));
            dataModel.put("outline_tpl", null);
            
            addUserInfoToModel(request, dataModel);
            dataModel.put("contextPath", request.getContextPath());
            
            // Dati richiesta
            dataModel.put("richiesta", richiesta);
            dataModel.put("page_title", getPageTitle(richiesta.getStato()));
            dataModel.put("page_icon", getPageIcon(richiesta.getStato()));
            
            // URL di ritorno
            String referer = request.getHeader("Referer");
            String backUrl = getBackUrl(referer, richiesta.getStato());
            dataModel.put("back_url", backUrl);
            dataModel.put("back_label", getBackLabel(richiesta.getStato()));
            
            // Messaggi
            String successParam = request.getParameter("success");
            String errorParam = request.getParameter("error");
            
            if (successParam != null && !successParam.trim().isEmpty()) {
                dataModel.put("success_message", successParam);
            }
            
            if (errorParam != null && !errorParam.trim().isEmpty()) {
                dataModel.put("error_message", getErrorMessage(errorParam));
            }
            
            new webengineering.framework.result.TemplateResult(getServletContext())
                    .activate("richiesta_details.ftl.html", dataModel, response);
                    
        } catch (TemplateManagerException ex) {
            logger.log(Level.SEVERE, "Errore template", ex);
            handleError(ex, request, response);
        }
    }
    
    private void handleAction(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        try {
            String action = request.getParameter("action");
            String richiestaIdParam = request.getParameter("richiesta_id");
            
            if (action == null || richiestaIdParam == null) {
                response.sendRedirect(request.getContextPath() + "/admin/dashboard?error=missing_params");
                return;
            }
            
            int richiestaId;
            try {
                richiestaId = Integer.parseInt(richiestaIdParam);
            } catch (NumberFormatException ex) {
                response.sendRedirect(request.getContextPath() + "/admin/dashboard?error=invalid_id");
                return;
            }
            
            // Ottieni DataLayer
            SoccorsoDataLayer dataLayer = (SoccorsoDataLayer) request.getAttribute("datalayer");
            if (dataLayer == null) {
                logger.severe("DataLayer non disponibile");
                response.sendRedirect(request.getContextPath() + "/admin/dashboard?error=system_error");
                return;
            }
            
            // Verifica esistenza richiesta
            RichiestaSoccorso richiesta = dataLayer.getRichiestaSoccorsoDAO().getRichiestaByCodice(richiestaId);
            if (richiesta == null) {
                response.sendRedirect(request.getContextPath() + "/admin/dashboard?error=not_found");
                return;
            }
            
            // Esegui azione
            String nuovoStato = null;
            String successMessage = null;
            
            if ("attiva".equals(action)) {
                if (!"Convalidata".equals(richiesta.getStato())) {
                    response.sendRedirect(request.getContextPath() + "/admin/richiesta/" + richiestaId + 
                                        "?error=invalid_state");
                    return;
                }
                
                // Aggiorna stato a "Attiva"
                dataLayer.getRichiestaSoccorsoDAO().updateStato(richiestaId, "Attiva");
                
                // Log operazione
                HttpSession session = request.getSession(false);
                String adminName = session != null ? (String) session.getAttribute("full_name") : "Admin";
                logger.info("Admin " + adminName + " ha attivato la richiesta " + richiestaId);
                
                // REDIRECT alla pagina di creazione missione invece che ai dettagli
                response.sendRedirect(request.getContextPath() + "/admin/NuovaMissione/" + richiestaId);
                return;
                
            } else if ("annulla".equals(action)) {
                if (!"Convalidata".equals(richiesta.getStato())) {
                    response.sendRedirect(request.getContextPath() + "/admin/richiesta/" + richiestaId + 
                                        "?error=invalid_state");
                    return;
                }
                nuovoStato = "Annullata";
                successMessage = "Richiesta annullata";
                
                // Aggiorna stato e redirect normale per annullamento
                dataLayer.getRichiestaSoccorsoDAO().updateStato(richiestaId, nuovoStato);
                
                // Log operazione
                HttpSession session2 = request.getSession(false);
                String adminName2 = session2 != null ? (String) session2.getAttribute("full_name") : "Admin";
                logger.info("Admin " + adminName2 + " ha annullato la richiesta " + richiestaId);
                
                // Redirect con successo per annullamento
                response.sendRedirect(request.getContextPath() + "/admin/richiesta/" + richiestaId + 
                                    "?success=" + java.net.URLEncoder.encode(successMessage, "UTF-8"));
                return;
            } else {
                response.sendRedirect(request.getContextPath() + "/admin/richiesta/" + richiestaId + 
                                    "?error=invalid_action");
                return;
            }
            
        } catch (DataException ex) {
            logger.log(Level.SEVERE, "Errore database azione", ex);
            try {
                response.sendRedirect(request.getContextPath() + "/admin/dashboard?error=database_error");
            } catch (Exception e) {
                handleError(ex, request, response);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico azione", ex);
            try {
                response.sendRedirect(request.getContextPath() + "/admin/dashboard?error=system_error");
            } catch (Exception e) {
                handleError(ex, request, response);
            }
        }
    }
    
    private String getBackUrl(String referer, String stato) {
        if (referer != null) {
            if (referer.contains("/admin/richieste/convalidate")) {
                return "/admin/richieste/convalidate";
            } else if (referer.contains("/admin/richieste/attive")) {
                return "/admin/richieste/attive";
            } else if (referer.contains("/admin/richieste/concluse")) {
                return "/admin/richieste/concluse";
            } else if (referer.contains("/admin/richieste/annullate")) {
                return "/admin/richieste/annullate";
            }
        }
        
        // Fallback basato sullo stato
        switch (stato) {
            case "Convalidata": return "/admin/richieste/convalidate";
            case "Attiva": return "/admin/richieste/attive";
            case "Conclusa": return "/admin/richieste/concluse";
            case "Annullata": return "/admin/richieste/annullate";
            default: return "/admin/dashboard";
        }
    }
    
    private String getPageTitle(String stato) {
        switch (stato) {
            case "Convalidata": return "Dettagli Richiesta Convalidata";
            case "Attiva": return "Dettagli Richiesta Attiva";
            case "Conclusa": return "Dettagli Richiesta Conclusa";
            case "Annullata": return "Dettagli Richiesta Annullata";
            default: return "Dettagli Richiesta";
        }
    }
    
    private String getPageIcon(String stato) {
        switch (stato) {
            case "Convalidata": return "📋";
            case "Attiva": return "⚡";
            case "Conclusa": return "✅";
            case "Annullata": return "❌";
            default: return "📄";
        }
    }
    
    private String getBackLabel(String stato) {
        switch (stato) {
            case "Convalidata": return "Torna alle Richieste Convalidate";
            case "Attiva": return "Torna alle Richieste Attive";
            case "Conclusa": return "Torna alle Richieste Concluse";
            case "Annullata": return "Torna alle Richieste Annullate";
            default: return "Torna alla Dashboard";
        }
    }
    
    private void showErrorPage(HttpServletRequest request, HttpServletResponse response, String message) 
            throws ServletException {
        
        try {
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("thispageurl", request.getAttribute("thispageurl"));
            dataModel.put("outline_tpl", null);
            
            addUserInfoToModel(request, dataModel);
            dataModel.put("error_message", message);
            dataModel.put("contextPath", request.getContextPath());
            
            new webengineering.framework.result.TemplateResult(getServletContext())
                    .activate("error.ftl.html", dataModel, response);
                    
        } catch (TemplateManagerException ex) {
            logger.log(Level.SEVERE, "Errore template errore", ex);
            handleError(ex, request, response);
        }
    }
    
    private String getErrorMessage(String errorCode) {
        switch (errorCode) {
            case "access_denied":
                return "Accesso negato. Solo gli amministratori possono accedere.";
            case "invalid_id":
                return "ID richiesta non valido.";
            case "not_found":
                return "Richiesta non trovata.";
            case "invalid_state":
                return "Azione non permessa per lo stato attuale della richiesta.";
            case "invalid_action":
                return "Azione non riconosciuta.";
            case "missing_params":
                return "Parametri mancanti nella richiesta.";
            case "database_error":
                return "Errore nel database. Riprova più tardi.";
            case "system_error":
                return "Errore di sistema. Riprova più tardi.";
            default:
                return "Si è verificato un errore.";
        }
    }
}