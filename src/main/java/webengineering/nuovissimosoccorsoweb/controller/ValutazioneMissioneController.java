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
import webengineering.nuovissimosoccorsoweb.model.Missione;
import webengineering.nuovissimosoccorsoweb.model.InfoMissione;

public class ValutazioneMissioneController extends SoccorsoBaseController {
    
    private static final Logger logger = Logger.getLogger(ValutazioneMissioneController.class.getName());
    
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
        
        // Gestisci in base al metodo HTTP
        if ("POST".equals(request.getMethod())) {
            // TODO: Gestire salvataggio valutazione (da implementare dopo)
            handleSaveValutazione(request, response);
        } else {
            // GET - mostra pagina valutazione
            showValutazionePage(request, response);
        }
    }
    
    private boolean isUserAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        
        String userRole = (String) session.getAttribute("user_role");
        return "admin".equals(userRole);
    }
    
    private void showValutazionePage(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        try {
            // Estrai ID missione dalla URL
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || pathInfo.length() <= 1) {
                response.sendRedirect(request.getContextPath() + "/admin/richieste/attive");
                return;
            }
            
            String idString = pathInfo.substring(1);
            if (idString.endsWith("/")) {
                idString = idString.substring(0, idString.length() - 1);
            }
            
            int missioneId;
            try {
                missioneId = Integer.parseInt(idString);
            } catch (NumberFormatException ex) {
                logger.warning("ID missione non valido: " + idString);
                response.sendRedirect(request.getContextPath() + "/admin/richieste/attive?error=invalid_id");
                return;
            }
            
            // Ottieni DataLayer
            SoccorsoDataLayer dataLayer = (SoccorsoDataLayer) request.getAttribute("datalayer");
            if (dataLayer == null) {
                logger.severe("DataLayer non disponibile");
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore sistema");
                return;
            }
            
            // Carica missione
            Missione missione = dataLayer.getMissioneDAO().getMissioneByCodice(missioneId);
            if (missione == null) {
                logger.warning("Missione non trovata: " + missioneId);
                response.sendRedirect(request.getContextPath() + "/admin/richieste/attive?error=not_found");
                return;
            }
            
            // Verifica che la missione sia "conclusa" (esiste già una valutazione)
            InfoMissione infoEsistente = dataLayer.getInfoMissioneDAO().getInfoByCodiceMissione(missioneId);
            if (infoEsistente != null) {
                // Missione già valutata - mostra valutazione esistente
                displayValutazioneEsistente(request, response, missione, infoEsistente);
            } else {
                // Missione non ancora valutata - mostra form per valutazione
                displayFormValutazione(request, response, missione);
            }
            
        } catch (DataException ex) {
            logger.log(Level.SEVERE, "Errore database", ex);
            showErrorPage(request, response, "Errore caricamento dati missione");
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico", ex);
            showErrorPage(request, response, "Errore di sistema");
        }
    }
    
    private void displayFormValutazione(HttpServletRequest request, HttpServletResponse response, 
                                      Missione missione) throws ServletException {
        
        try {
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("thispageurl", request.getAttribute("thispageurl"));
            dataModel.put("outline_tpl", null);
            
            addUserInfoToModel(request, dataModel);
            dataModel.put("contextPath", request.getContextPath());
            
            // Dati missione
            dataModel.put("missione", missione);
            dataModel.put("form_mode", "create"); // Indica che stiamo creando una nuova valutazione
            
            // Info pagina
            dataModel.put("page_title", "Valutazione Missione #" + missione.getCodiceRichiesta());
            dataModel.put("page_icon", "⭐");
            
            // Gestisci messaggi
            String errorParam = request.getParameter("error");
            if (errorParam != null && !errorParam.trim().isEmpty()) {
                dataModel.put("error_message", getErrorMessage(errorParam));
            }
            
            String successParam = request.getParameter("success");
            if (successParam != null && !successParam.trim().isEmpty()) {
                dataModel.put("success_message", getSuccessMessage(successParam));
            }
            
            new webengineering.framework.result.TemplateResult(getServletContext())
                    .activate("valutazione_missione.ftl.html", dataModel, response);
                    
        } catch (TemplateManagerException ex) {
            logger.log(Level.SEVERE, "Errore template", ex);
            handleError(ex, request, response);
        }
    }
    
    private void displayValutazioneEsistente(HttpServletRequest request, HttpServletResponse response, 
                                           Missione missione, InfoMissione info) throws ServletException {
        
        try {
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("thispageurl", request.getAttribute("thispageurl"));
            dataModel.put("outline_tpl", null);
            
            addUserInfoToModel(request, dataModel);
            dataModel.put("contextPath", request.getContextPath());
            
            // Dati missione e valutazione
            dataModel.put("missione", missione);
            dataModel.put("info_missione", info);
            dataModel.put("form_mode", "view"); // Indica che stiamo solo visualizzando
            
            // Info pagina
            dataModel.put("page_title", "Valutazione Missione #" + missione.getCodiceRichiesta());
            dataModel.put("page_icon", "⭐");
            
            // Gestisci messaggi
            String errorParam = request.getParameter("error");
            if (errorParam != null && !errorParam.trim().isEmpty()) {
                dataModel.put("error_message", getErrorMessage(errorParam));
            }
            
            new webengineering.framework.result.TemplateResult(getServletContext())
                    .activate("valutazione_missione.ftl.html", dataModel, response);
                    
        } catch (TemplateManagerException ex) {
            logger.log(Level.SEVERE, "Errore template", ex);
            handleError(ex, request, response);
        }
    }
    
    private void handleSaveValutazione(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        try {
            // Estrai ID dalla URL
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || pathInfo.length() <= 1) {
                response.sendRedirect(request.getContextPath() + "/admin/richieste/attive?error=invalid_request");
                return;
            }
            
            String idString = pathInfo.substring(1);
            if (idString.endsWith("/")) {
                idString = idString.substring(0, idString.length() - 1);
            }
            
            int missioneId;
            try {
                missioneId = Integer.parseInt(idString);
            } catch (NumberFormatException ex) {
                logger.warning("ID missione non valido per salvataggio: " + idString);
                response.sendRedirect(request.getContextPath() + "/admin/richieste/attive?error=invalid_id");
                return;
            }
            
            // Estrai parametri dal form
            String successoStr = request.getParameter("successo");
            String commento = request.getParameter("commento");
            
            // Validazione parametri
            if (successoStr == null || successoStr.trim().isEmpty()) {
                logger.warning("Valutazione mancante per missione: " + missioneId);
                response.sendRedirect(request.getContextPath() + "/admin/valutazione/" + missioneId + "/?error=missing_rating");
                return;
            }
            
            int successo;
            try {
                successo = Integer.parseInt(successoStr);
                if (successo < 1 || successo > 5) {
                    throw new NumberFormatException("Valore non valido: " + successo);
                }
            } catch (NumberFormatException ex) {
                logger.warning("Valutazione non valida per missione " + missioneId + ": " + successoStr);
                response.sendRedirect(request.getContextPath() + "/admin/valutazione/" + missioneId + "/?error=invalid_rating");
                return;
            }
            
            // Sanitizza commento (null -> stringa vuota)
            if (commento == null) {
                commento = "";
            }
            
            // Ottieni DataLayer
            SoccorsoDataLayer dataLayer = (SoccorsoDataLayer) request.getAttribute("datalayer");
            if (dataLayer == null) {
                logger.severe("DataLayer non disponibile");
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore sistema");
                return;
            }
            
            // Verifica che la missione esista
            Missione missione = dataLayer.getMissioneDAO().getMissioneByCodice(missioneId);
            if (missione == null) {
                logger.warning("Tentativo di valutare missione inesistente: " + missioneId);
                response.sendRedirect(request.getContextPath() + "/admin/richieste/attive?error=not_found");
                return;
            }
            
            // Verifica che la missione non sia già stata valutata
            InfoMissione infoEsistente = dataLayer.getInfoMissioneDAO().getInfoByCodiceMissione(missioneId);
            if (infoEsistente != null) {
                logger.warning("Tentativo di rivalutare missione già valutata: " + missioneId);
                response.sendRedirect(request.getContextPath() + "/admin/valutazione/" + missioneId + "/?error=already_evaluated");
                return;
            }
            
            // Crea oggetto InfoMissione
            InfoMissione infoMissione = new webengineering.nuovissimosoccorsoweb.model.impl.InfoMissioneImpl();
            infoMissione.setCodiceMissione(missioneId);
            infoMissione.setSuccesso(successo);
            infoMissione.setCommento(commento);
            infoMissione.setDataOraFine(java.time.LocalDateTime.now()); // Timestamp corrente
            infoMissione.setVersion(1);
            
            // Salva nel database
            dataLayer.getInfoMissioneDAO().storeInfoMissione(infoMissione);
            
            // Cambia lo stato della richiesta da "Attiva" a "Conclusa"
            try {
                dataLayer.getRichiestaSoccorsoDAO().updateStato(missione.getCodiceRichiesta(), "Chiusa");
                logger.info("Stato richiesta " + missione.getCodiceRichiesta() + " cambiato da 'Attiva' a 'Chiusa'");
            } catch (DataException ex) {
                logger.log(Level.WARNING, "Errore nel cambio stato richiesta " + missione.getCodiceRichiesta(), ex);
                // Non bloccare il processo - la valutazione è comunque salvata
            }
            
            logger.info("Valutazione salvata per missione " + missioneId + " - Successo: " + successo + 
                       ", Commento: " + commento.length() + " caratteri");
            
            // Redirect con messaggio di successo
            response.sendRedirect(request.getContextPath() + "/admin/valutazione/" + missioneId + "/?success=evaluation_saved");
            
        } catch (DataException ex) {
            logger.log(Level.SEVERE, "Errore database durante salvataggio valutazione", ex);
            try {
                String pathInfo = request.getPathInfo();
                String idString = pathInfo.substring(1);
                if (idString.endsWith("/")) {
                    idString = idString.substring(0, idString.length() - 1);
                }
                response.sendRedirect(request.getContextPath() + "/admin/valutazione/" + idString + "/?error=database_error");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore redirect dopo errore database", e);
                handleError(ex, request, response);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico durante salvataggio valutazione", ex);
            try {
                String pathInfo = request.getPathInfo();
                String idString = pathInfo.substring(1);
                if (idString.endsWith("/")) {
                    idString = idString.substring(0, idString.length() - 1);
                }
                response.sendRedirect(request.getContextPath() + "/admin/valutazione/" + idString + "/?error=system_error");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore redirect dopo errore generico", e);
                handleError(ex, request, response);
            }
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
                return "ID missione non valido.";
            case "not_found":
                return "Missione non trovata.";
            case "already_concluded":
                return "Missione già conclusa e valutata.";
            case "already_evaluated":
                return "Questa missione è già stata valutata.";
            case "missing_rating":
                return "Seleziona una valutazione da 1 a 5 stelle.";
            case "invalid_rating":
                return "Valutazione non valida. Seleziona un valore da 1 a 5.";
            case "invalid_request":
                return "Richiesta non valida.";
            case "database_error":
                return "Errore nel database. Riprova più tardi.";
            case "system_error":
                return "Errore di sistema. Riprova più tardi.";
            default:
                return "Si è verificato un errore: " + errorCode;
        }
    }
    
    private String getSuccessMessage(String successCode) {
        switch (successCode) {
            case "evaluation_saved":
                return "Valutazione salvata con successo! La missione è ora conclusa.";
            case "valutazione_saved":
                return "Valutazione salvata con successo!";
            default:
                return successCode;
        }
    }
}