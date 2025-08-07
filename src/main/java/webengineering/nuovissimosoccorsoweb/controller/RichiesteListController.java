package webengineering.nuovissimosoccorsoweb.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import webengineering.framework.result.TemplateManagerException;
import webengineering.nuovissimosoccorsoweb.SoccorsoDataLayer;
import webengineering.framework.data.DataException;
import webengineering.nuovissimosoccorsoweb.model.InfoMissione;
import webengineering.nuovissimosoccorsoweb.model.RichiestaSoccorso;
import webengineering.nuovissimosoccorsoweb.model.Missione;

public class RichiesteListController extends SoccorsoBaseController {
    
    private static final Logger logger = Logger.getLogger(RichiesteListController.class.getName());
    
    @Override
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        // Verifica che l'utente sia un amministratore
        if (!isUserAdmin(request)) {
            try {
                response.sendRedirect(request.getContextPath() + "/login?error=access_denied");
                return;
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Errore nel redirect per accesso negato", ex);
                handleError(ex, request, response);
                return;
            }
        }
        
        // Estrai il tipo di richieste dalla URL
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.length() <= 1) {
            try {
                response.sendRedirect(request.getContextPath() + "/admin/dashboard");
                return;
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Errore nel redirect", ex);
                handleError(ex, request, response);
                return;
            }
        }
        
        String tipoRichieste = pathInfo.substring(1); // Rimuove il '/' iniziale
        
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
        
        try {
            // Se è "attive", mostra solo le missioni con richieste attive
            if ("attive".equals(tipoRichieste)) {
                showMissioniAttive(request, response, dataLayer);
            } else if ("concluse".equals(tipoRichieste)) {
                // NUOVO: Mostra le missioni concluse (info_missione)
                showMissioniConcluse(request, response, dataLayer);
            } else {
                // Per tutti gli altri tipi, mostra le richieste normali
                showRichiesteList(request, response, dataLayer, tipoRichieste);
            }
            
        } catch (DataException ex) {
            logger.log(Level.SEVERE, "Errore database nella lista richieste", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nel caricamento dei dati");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico nella lista richieste", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore di sistema");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        }
    }
    
    // Verifica se l'utente è un amministratore
    private boolean isUserAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        
        String userRole = (String) session.getAttribute("user_role");
        return "admin".equals(userRole);
    }
    
    // Mostra solo le missioni con richieste attive
    private void showMissioniAttive(HttpServletRequest request, HttpServletResponse response, 
                                   SoccorsoDataLayer dataLayer) 
            throws ServletException, DataException {
        
        // 1. Recupera tutte le richieste con stato "Attiva"
        List<RichiestaSoccorso> richiesteAttive = dataLayer.getRichiestaSoccorsoDAO().getRichiesteByStato("Attiva");
        
        // 2. Per ogni richiesta attiva, verifica se esiste una missione corrispondente
        List<Missione> missioniAttive = new ArrayList<>();
        for (RichiestaSoccorso richiesta : richiesteAttive) {
            try {
                Missione missione = dataLayer.getMissioneDAO().getMissioneByCodice(richiesta.getCodice());
                if (missione != null) {
                    missioniAttive.add(missione);
                }
            } catch (DataException ex) {
                logger.log(Level.WARNING, "Errore nel recupero missione per richiesta " + richiesta.getCodice(), ex);
                // Continua con le altre richieste
            }
        }
        
        logger.info("Trovate " + richiesteAttive.size() + " richieste attive, " + 
                   missioniAttive.size() + " missioni corrispondenti");
        
        // Prepara i dati per il template
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("thispageurl", request.getAttribute("thispageurl"));
        dataModel.put("outline_tpl", null);
        
        addUserInfoToModel(request, dataModel);
        
        // Aggiungi context path esplicitamente
        dataModel.put("contextPath", request.getContextPath());
        
        // Dati specifici della lista missioni
        dataModel.put("missioni", missioniAttive);
        dataModel.put("count_missioni", missioniAttive.size());
        dataModel.put("is_missioni_view", true); // Flag per distinguere nel template
        
        // Informazioni per il template
        dataModel.put("page_title", "Missioni Attive");
        dataModel.put("page_icon", "🚀");
        dataModel.put("page_description", "Missioni in corso per richieste con stato 'Attiva'");
        
        // Gestisci messaggi
        String successParam = request.getParameter("success");
        String errorParam = request.getParameter("error");
        
        if (successParam != null && !successParam.trim().isEmpty()) {
            dataModel.put("success_message", getSuccessMessage(successParam));
        }
        
        if (errorParam != null && !errorParam.trim().isEmpty()) {
            dataModel.put("error_message", getErrorMessage(errorParam));
        }
        
        try {
            new webengineering.framework.result.TemplateResult(getServletContext())
                    .activate("missioni_list.ftl.html", dataModel, response);
        } catch (TemplateManagerException ex) {
            logger.log(Level.SEVERE, "Errore nel template della lista missioni", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nel caricamento della pagina");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        }
    }
    
    // Mostra le missioni concluse (info_missione)
    private void showMissioniConcluse(HttpServletRequest request, HttpServletResponse response, 
                                     SoccorsoDataLayer dataLayer) 
            throws ServletException, DataException {
        
        // Carica tutte le info_missione
        List<InfoMissione> infoMissioni = dataLayer.getInfoMissioneDAO().getAllInfoMissioni();
        
        // Per ogni info_missione, carica anche i dati della missione corrispondente
        Map<Integer, Missione> missioniMap = new HashMap<>();
        for (InfoMissione info : infoMissioni) {
            try {
                Missione missione = dataLayer.getMissioneDAO().getMissioneByCodice(info.getCodiceMissione());
                if (missione != null) {
                    missioniMap.put(info.getCodiceMissione(), missione);
                }
            } catch (DataException ex) {
                logger.log(Level.WARNING, "Errore nel recupero missione per info " + info.getCodiceMissione(), ex);
            }
        }
        
        logger.info("Trovate " + infoMissioni.size() + " missioni concluse");
        
        // Prepara i dati per il template
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("thispageurl", request.getAttribute("thispageurl"));
        dataModel.put("outline_tpl", null);
        
        addUserInfoToModel(request, dataModel);
        dataModel.put("contextPath", request.getContextPath());
        
        // Dati specifici delle missioni concluse
        dataModel.put("info_missioni", infoMissioni);
        dataModel.put("missioni_map", missioniMap);
        dataModel.put("count_concluse", infoMissioni.size());
        dataModel.put("is_concluse_view", true); // Flag per il template
        
        // Informazioni per il template
        dataModel.put("page_title", "Missioni Concluse");
        dataModel.put("page_icon", "✅");
        dataModel.put("page_description", "Missioni completate e valutate");
        
        // Gestisci messaggi
        String successParam = request.getParameter("success");
        String errorParam = request.getParameter("error");
        
        if (successParam != null && !successParam.trim().isEmpty()) {
            dataModel.put("success_message", getSuccessMessage(successParam));
        }
        
        if (errorParam != null && !errorParam.trim().isEmpty()) {
            dataModel.put("error_message", getErrorMessage(errorParam));
        }
        
        try {
            new webengineering.framework.result.TemplateResult(getServletContext())
                    .activate("missioni_concluse.ftl.html", dataModel, response);
        } catch (TemplateManagerException ex) {
            logger.log(Level.SEVERE, "Errore nel template delle missioni concluse", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nel caricamento della pagina");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        }
    }
    private void showRichiesteList(HttpServletRequest request, HttpServletResponse response, 
                                  SoccorsoDataLayer dataLayer, String tipoRichieste) 
            throws ServletException, DataException {
        
        // Determina lo stato da cercare nel database
        String statoDb = mapTipoToStato(tipoRichieste);
        if (statoDb == null) {
            try {
                response.sendRedirect(request.getContextPath() + "/admin/dashboard?error=invalid_type");
                return;
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Errore nel redirect", ex);
                return;
            }
        }
        
        // Carica le richieste
        List<RichiestaSoccorso> richieste = dataLayer.getRichiestaSoccorsoDAO().getRichiesteByStato(statoDb);
        
        // Prepara i dati per il template
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("thispageurl", request.getAttribute("thispageurl"));
        dataModel.put("outline_tpl", null);
        
        addUserInfoToModel(request, dataModel);
        
        // Aggiungi context path esplicitamente
        dataModel.put("contextPath", request.getContextPath());
        
        // Dati specifici della lista
        dataModel.put("richieste", richieste);
        dataModel.put("tipo_richieste", tipoRichieste);
        dataModel.put("stato_db", statoDb);
        dataModel.put("count_richieste", richieste.size());
        dataModel.put("is_missioni_view", false); // Flag per distinguere nel template
        
        // Informazioni per il template
        dataModel.put("page_title", getPageTitle(tipoRichieste));
        dataModel.put("page_icon", getPageIcon(tipoRichieste));
        dataModel.put("page_description", getPageDescription(tipoRichieste));
        
        // Gestisci messaggi
        String successParam = request.getParameter("success");
        String errorParam = request.getParameter("error");
        
        if (successParam != null && !successParam.trim().isEmpty()) {
            dataModel.put("success_message", getSuccessMessage(successParam));
        }
        
        if (errorParam != null && !errorParam.trim().isEmpty()) {
            dataModel.put("error_message", getErrorMessage(errorParam));
        }
        
        try {
            new webengineering.framework.result.TemplateResult(getServletContext())
                    .activate("richieste_list.ftl.html", dataModel, response);
        } catch (TemplateManagerException ex) {
            logger.log(Level.SEVERE, "Errore nel template della lista", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nel caricamento della pagina");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        }
    }
    
    // Mappa i tipi URL agli stati del database
    private String mapTipoToStato(String tipo) {
        switch (tipo) {
            case "convalidate": return "Convalidata";
            case "annullate": return "Annullata";
            case "chiuse": return "Chiusa";  // Aggiornato da "concluse" a "chiuse"
            default: return null;
        }
    }
    
    // Ottiene il titolo della pagina
    private String getPageTitle(String tipo) {
        switch (tipo) {
            case "convalidate": return "Richieste Convalidate";
            case "annullate": return "Richieste Annullate";
            case "chiuse": return "Richieste Chiuse";  // Aggiornato
            default: return "Lista Richieste";
        }
    }
    
    // Ottiene l'icona della pagina
    private String getPageIcon(String tipo) {
        switch (tipo) {
            case "convalidate": return "📋";
            case "annullate": return "❌";
            case "chiuse": return "✅";  // Aggiornato
            default: return "📄";
        }
    }
    
    // Ottiene la descrizione della pagina
    private String getPageDescription(String tipo) {
        switch (tipo) {
            case "convalidate": return "Richieste confermate dai segnalanti";
            case "annullate": return "Richieste che sono state annullate";
            case "chiuse": return "Richieste completate con successo";  // Aggiornato
            default: return "Lista delle richieste di soccorso";
        }
    }
    
    // Messaggi di successo
    private String getSuccessMessage(String successCode) {
        switch (successCode) {
            case "evaluation_saved":
                return "Valutazione salvata con successo! La missione è stata conclusa.";
            case "mission_concluded":
                return "Missione conclusa con successo.";
            default:
                return successCode;
        }
    }
    
    // Messaggi di errore
    private String getErrorMessage(String errorCode) {
        switch (errorCode) {
            case "access_denied":
                return "Accesso negato. Solo gli amministratori possono accedere.";
            case "invalid_type":
                return "Tipo di richiesta non valido.";
            case "database_error":
                return "Errore nel database. Riprova più tardi.";
            case "system_error":
                return "Errore di sistema. Riprova più tardi.";
            default:
                return "Si è verificato un errore.";
        }
    }
}