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
import webengineering.nuovissimosoccorsoweb.model.Missione;
import webengineering.nuovissimosoccorsoweb.model.RichiestaSoccorso;
import webengineering.nuovissimosoccorsoweb.model.InfoMissione;
import webengineering.nuovissimosoccorsoweb.model.PartecipazioneSquadra;

public class OperatoreStoricoController extends SoccorsoBaseController {
    
    private static final Logger logger = Logger.getLogger(OperatoreStoricoController.class.getName());
    
    @Override
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        logger.info("=== OperatoreStoricoController - processRequest chiamato ===");
        
        // Verifica che l'utente sia un operatore
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
        
        try {
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
            
            // Mostra lo storico missioni operatore
            showStoricoMissioni(request, response, dataLayer);
            
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico nello storico missioni operatore", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore di sistema");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        }
    }
    
    private boolean isUserOperatore(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        
        String userRole = (String) session.getAttribute("user_role");
        return "operatore".equals(userRole) || "operator".equals(userRole);
    }
    
    private void showStoricoMissioni(HttpServletRequest request, HttpServletResponse response, 
                                    SoccorsoDataLayer dataLayer) throws ServletException {
        
        logger.info("=== showStoricoMissioni ===");
        
        try {
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("thispageurl", request.getAttribute("thispageurl"));
            dataModel.put("outline_tpl", null);
            
            addUserInfoToModel(request, dataModel);
            dataModel.put("contextPath", request.getContextPath());
            
            // Gestisci messaggi
            String successParam = request.getParameter("success");
            String errorParam = request.getParameter("error");
            
            if (successParam != null && !successParam.trim().isEmpty()) {
                dataModel.put("success_message", getSuccessMessage(successParam));
            }
            
            if (errorParam != null && !errorParam.trim().isEmpty()) {
                dataModel.put("error_message", getErrorMessage(errorParam));
            }
            
            // Informazioni operatore
            HttpSession session = request.getSession(false);
            if (session != null) {
                String fullName = (String) session.getAttribute("full_name");
                Object operatoreIdObj = session.getAttribute("userid");
                String operatoreId = null;
                
                if (operatoreIdObj instanceof Integer) {
                    operatoreId = String.valueOf(operatoreIdObj);
                } else if (operatoreIdObj instanceof String) {
                    operatoreId = (String) operatoreIdObj;
                }
                
                dataModel.put("operatore_name", fullName != null ? fullName : "Operatore");
                dataModel.put("operatore_id", operatoreId);
                
                if (operatoreId != null) {
                    List<MissioneConDettagli> storicoMissioni = loadStoricoMissioni(dataLayer, operatoreId);
                    dataModel.put("storico_missioni", storicoMissioni);
                    
                    // Assicurati che total_missioni sia un Integer
                    Integer totalMissioni = storicoMissioni.size();
                    dataModel.put("total_missioni", totalMissioni);
                    
                    logger.info("Trovate " + totalMissioni + " missioni per operatore " + operatoreId);
                } else {
                    logger.warning("ID operatore non disponibile in sessione");
                    dataModel.put("storico_missioni", new ArrayList<>());
                    dataModel.put("total_missioni", Integer.valueOf(0));
                }
            }
            
            logger.info("Caricamento template operatore_storico.ftl.html");
            
            new webengineering.framework.result.TemplateResult(getServletContext())
                    .activate("operatore_storico.ftl.html", dataModel, response);
                    
            logger.info("Template caricato con successo");
                    
        } catch (TemplateManagerException ex) {
            logger.log(Level.SEVERE, "Errore nel template storico operatore", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nel caricamento della pagina");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico in showStoricoMissioni", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore di sistema");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        }
    }
    
    private List<MissioneConDettagli> loadStoricoMissioni(SoccorsoDataLayer dataLayer, String operatoreId) throws DataException {
        List<MissioneConDettagli> missioniConDettagli = new ArrayList<>();
        
        try {
            int operatoreIdInt = Integer.parseInt(operatoreId);
            
            List<Missione> missioni = getMissioniByOperatore(operatoreIdInt, dataLayer);
            
            for (Missione missione : missioni) {
                try {
                    RichiestaSoccorso richiesta = 
                        dataLayer.getRichiestaSoccorsoDAO().getRichiestaByCodice(missione.getCodiceRichiesta());
                    
                    InfoMissione infoMissione = null;
                    try {
                        infoMissione = dataLayer.getInfoMissioneDAO().getInfoByCodiceMissione(missione.getCodiceRichiesta());
                    } catch (DataException ex) {
                        logger.log(Level.WARNING, "InfoMissione non trovata per missione " + missione.getCodiceRichiesta(), ex);
                    }
                    
                    String ruoloSquadra = null;
                    try {
                        List<PartecipazioneSquadra> squadra = dataLayer.getMissioneDAO().getSquadraByMissione(missione.getCodiceRichiesta());
                        for (PartecipazioneSquadra partecipazione : squadra) {
                            if (partecipazione.getIdOperatore() == operatoreIdInt) {
                                ruoloSquadra = partecipazione.getRuolo().toString();
                                break;
                            }
                        }
                    } catch (DataException ex) {
                        logger.log(Level.WARNING, "Errore nel caricamento ruolo per missione " + missione.getCodiceRichiesta(), ex);
                    }
                    
                    missioniConDettagli.add(new MissioneConDettagli(missione, richiesta, infoMissione, ruoloSquadra));
                } catch (DataException ex) {
                    logger.log(Level.WARNING, "Errore nel caricamento dettagli per missione " + missione.getCodiceRichiesta(), ex);
                    missioniConDettagli.add(new MissioneConDettagli(missione, null, null, null));
                }
            }
            
        } catch (NumberFormatException ex) {
            logger.log(Level.WARNING, "ID operatore non valido: " + operatoreId, ex);
            throw new DataException("ID operatore non valido", ex);
        }
        
        return missioniConDettagli;
    }
    
    private List<Missione> getMissioniByOperatore(int operatoreId, SoccorsoDataLayer dataLayer) throws DataException {
        return dataLayer.getMissioneDAO().getMissioniByOperatore(operatoreId);
    }
    
    private String getSuccessMessage(String successCode) {
        switch (successCode) {
            case "mission_viewed":
                return "Dettagli missione visualizzati.";
            default:
                return "Operazione completata con successo.";
        }
    }
    
    private String getErrorMessage(String errorCode) {
        switch (errorCode) {
            case "access_denied":
                return "Accesso negato. Solo gli operatori possono accedere.";
            case "database_error":
                return "Errore nel database. Riprova più tardi.";
            case "mission_not_found":
                return "Missione non trovata.";
            default:
                return "Si è verificato un errore.";
        }
    }
    
    public static class MissioneConDettagli {
        private final Missione missione;
        private final RichiestaSoccorso richiesta;
        private final InfoMissione infoMissione;
        private final String ruoloSquadra;
        
        public MissioneConDettagli(Missione missione, 
                                  RichiestaSoccorso richiesta, 
                                  InfoMissione infoMissione,
                                  String ruoloSquadra) {
            this.missione = missione;
            this.richiesta = richiesta;
            this.infoMissione = infoMissione;
            this.ruoloSquadra = ruoloSquadra;
        }
        
        public Missione getMissione() {
            return missione;
        }
        
        public RichiestaSoccorso getRichiesta() {
            return richiesta;
        }
        
        public InfoMissione getInfoMissione() {
            return infoMissione;
        }
        
        public String getRuoloSquadra() {
            return ruoloSquadra;
        }
    }
}