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
import webengineering.nuovissimosoccorsoweb.model.InfoMissione;
import webengineering.nuovissimosoccorsoweb.model.Missione;
import webengineering.nuovissimosoccorsoweb.model.PartecipazioneSquadra;
import webengineering.nuovissimosoccorsoweb.model.Mezzo;
import webengineering.nuovissimosoccorsoweb.model.Materiale;
import webengineering.nuovissimosoccorsoweb.model.Operatore;

public class MissioneDetailsController extends SoccorsoBaseController {
    
    private static final Logger logger = Logger.getLogger(MissioneDetailsController.class.getName());
    
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
        
        // Controlla se è una richiesta di conclusione
        String pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.endsWith("/conclude")) {
            handleConcludeMission(request, response);
            return;
        }
        
        // Gestisci in base al metodo HTTP
        if ("POST".equals(request.getMethod())) {
            handleSaveNotes(request, response);
        } else {
            // GET - mostra dettagli
            showMissionDetails(request, response);
        }
    }
    
    private void handleConcludeMission(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        if (!"POST".equals(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        
        try {
            // Estrai ID dalla URL (rimuovi /conclude)
            String pathInfo = request.getPathInfo();
            String idString = pathInfo.substring(1, pathInfo.length() - 9); // rimuovi "/" iniziale e "/conclude"
            
            int missioneId;
            try {
                missioneId = Integer.parseInt(idString);
            } catch (NumberFormatException ex) {
                logger.warning("ID missione non valido per conclusione: " + idString);
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
            
            // Verifica che la missione esista
            Missione missione = dataLayer.getMissioneDAO().getMissioneByCodice(missioneId);
            if (missione == null) {
                logger.warning("Tentativo di concludere missione inesistente: " + missioneId);
                response.sendRedirect(request.getContextPath() + "/admin/richieste/attive?error=not_found");
                return;
            }
            
            // Verifica che la missione non sia già conclusa
            InfoMissione infoEsistente = dataLayer.getInfoMissioneDAO().getInfoByCodiceMissione(missioneId);
            if (infoEsistente != null) {
                logger.warning("Tentativo di concludere missione già conclusa: " + missioneId);
                response.sendRedirect(request.getContextPath() + "/admin/valutazione/" + missioneId + "/?error=already_concluded");
                return;
            }
            
            // TODO: Qui dovrebbe cambiare lo stato della RICHIESTA da "Attiva" a "Conclusa"
            // Per ora assumo che questo step sia gestito altrove o non necessario
            
            logger.info("Missione " + missioneId + " pronta per valutazione");
            
            // Redirect alla pagina di valutazione
            response.sendRedirect(request.getContextPath() + "/admin/valutazione/" + missioneId + "/");
            
        } catch (DataException ex) {
            logger.log(Level.SEVERE, "Errore database durante conclusione missione", ex);
            try {
                response.sendRedirect(request.getContextPath() + "/admin/richieste/attive?error=database_error");
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Errore redirect dopo errore database", e);
                handleError(ex, request, response);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico durante conclusione missione", ex);
            try {
                response.sendRedirect(request.getContextPath() + "/admin/richieste/attive?error=system_error");
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Errore redirect dopo errore generico", e);
                handleError(ex, request, response);
            }
        }
    }
    
    private void handleSaveNotes(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        try {
            // Estrai parametri
            String missioneIdStr = request.getParameter("missioneId");
            String note = request.getParameter("note");
            
            if (missioneIdStr == null || missioneIdStr.trim().isEmpty()) {
                logger.warning("ID missione mancante nella richiesta POST");
                response.sendRedirect(request.getContextPath() + "/admin/richieste/attive?error=invalid_request");
                return;
            }
            
            int missioneId;
            try {
                missioneId = Integer.parseInt(missioneIdStr);
            } catch (NumberFormatException ex) {
                logger.warning("ID missione non valido: " + missioneIdStr);
                response.sendRedirect(request.getContextPath() + "/admin/richieste/attive?error=invalid_id");
                return;
            }
            
            // Sanitizza le note (null -> stringa vuota)
            if (note == null) {
                note = "";
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
                logger.warning("Tentativo di aggiornare note per missione inesistente: " + missioneId);
                response.sendRedirect(request.getContextPath() + "/admin/richieste/attive?error=not_found");
                return;
            }
            
            // Aggiorna le note
            dataLayer.getMissioneDAO().updateNoteMissione(missioneId, note);
            
            logger.info("Note aggiornate per missione " + missioneId + " - Lunghezza: " + note.length() + " caratteri");
            
            // Redirect con messaggio di successo
            response.sendRedirect(request.getContextPath() + request.getPathInfo() + "?success=note_saved");
            
        } catch (DataException ex) {
            logger.log(Level.SEVERE, "Errore database durante salvataggio note", ex);
            try {
                response.sendRedirect(request.getContextPath() + request.getPathInfo() + "?error=database_error");
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Errore redirect dopo errore database", e);
                handleError(ex, request, response);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico durante salvataggio note", ex);
            try {
                response.sendRedirect(request.getContextPath() + request.getPathInfo() + "?error=system_error");
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Errore redirect dopo errore generico", e);
                handleError(ex, request, response);
            }
        }
    }
    
    private boolean isUserAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        
        String userRole = (String) session.getAttribute("user_role");
        return "admin".equals(userRole);
    }
    
    private void showMissionDetails(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        try {
            // Estrai ID missione dalla URL
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || pathInfo.length() <= 1) {
                response.sendRedirect(request.getContextPath() + "/admin/richieste/attive");
                return;
            }
            
            String idString = pathInfo.substring(1);
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
            
            // Carica squadra con dettagli operatori
            List<PartecipazioneSquadra> squadra = dataLayer.getMissioneDAO().getSquadraByMissione(missioneId);
            Map<String, Operatore> operatoriDettagli = new HashMap<>();
            
            for (PartecipazioneSquadra partecipazione : squadra) {
                try {
                    Operatore operatore = dataLayer.getOperatoreDAO().getOperatoreById(partecipazione.getIdOperatore());
                    if (operatore != null) {
                        // Usa String come chiave per compatibilità FreeMarker
                        operatoriDettagli.put(String.valueOf(partecipazione.getIdOperatore()), operatore);
                    }
                } catch (DataException ex) {
                    logger.log(Level.WARNING, "Errore caricamento operatore " + partecipazione.getIdOperatore(), ex);
                }
            }
            
            // Carica mezzi utilizzati
            List<Mezzo> mezzi = dataLayer.getMissioneDAO().getMezziByMissione(missioneId);
            
            // Carica materiali utilizzati
            List<Materiale> materiali = dataLayer.getMissioneDAO().getMaterialiByMissione(missioneId);
            
            logger.info("Dettagli missione " + missioneId + " caricati - Squadra: " + squadra.size() + 
                       ", Mezzi: " + mezzi.size() + ", Materiali: " + materiali.size());
            
            // Mostra pagina dettagli
            displayMissionDetails(request, response, missione, squadra, operatoriDettagli, mezzi, materiali);
            
        } catch (DataException ex) {
            logger.log(Level.SEVERE, "Errore database", ex);
            showErrorPage(request, response, "Errore caricamento dati missione");
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico", ex);
            showErrorPage(request, response, "Errore di sistema");
        }
    }
    
    private void displayMissionDetails(HttpServletRequest request, HttpServletResponse response, 
                                     Missione missione,
                                     List<PartecipazioneSquadra> squadra,
                                     Map<String, Operatore> operatoriDettagli,
                                     List<Mezzo> mezzi,
                                     List<Materiale> materiali) throws ServletException {
        
        try {
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("thispageurl", request.getAttribute("thispageurl"));
            dataModel.put("outline_tpl", null);
            
            addUserInfoToModel(request, dataModel);
            dataModel.put("contextPath", request.getContextPath());
            
            // Dati missione
            dataModel.put("missione", missione);
            
            // Squadra con dettagli
            dataModel.put("squadra", squadra);
            dataModel.put("operatori_dettagli", operatoriDettagli);
            
            // Risorse
            dataModel.put("mezzi", mezzi);
            dataModel.put("materiali", materiali);
            
            // Conteggi per statistiche
            dataModel.put("count_operatori", squadra.size());
            dataModel.put("count_mezzi", mezzi.size());
            dataModel.put("count_materiali", materiali.size());
            
            // Info pagina
            dataModel.put("page_title", "Dettagli Missione #" + missione.getCodiceRichiesta());
            dataModel.put("page_icon", "🎯");
            
            // Gestisci messaggi di errore se presenti
            String errorParam = request.getParameter("error");
            if (errorParam != null && !errorParam.trim().isEmpty()) {
                dataModel.put("error_message", getErrorMessage(errorParam));
            }
            
            String successParam = request.getParameter("success");
            if (successParam != null && !successParam.trim().isEmpty()) {
                dataModel.put("success_message", getSuccessMessage(successParam));
            }
            
            new webengineering.framework.result.TemplateResult(getServletContext())
                    .activate("missione_details.ftl.html", dataModel, response);
                    
        } catch (TemplateManagerException ex) {
            logger.log(Level.SEVERE, "Errore template", ex);
            handleError(ex, request, response);
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
            case "database_error":
                return "Errore nel database. Riprova più tardi.";
            case "system_error":
                return "Errore di sistema. Riprova più tardi.";
            case "invalid_request":
                return "Richiesta non valida.";
            default:
                return "Si è verificato un errore: " + errorCode;
        }
    }
    
    private String getSuccessMessage(String successCode) {
        switch (successCode) {
            case "note_saved":
                return "Note salvate con successo!";
            default:
                return successCode;
        }
    }
}