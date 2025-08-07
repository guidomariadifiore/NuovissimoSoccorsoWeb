package webengineering.nuovissimosoccorsoweb.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import webengineering.framework.result.TemplateManagerException;
import webengineering.nuovissimosoccorsoweb.SoccorsoDataLayer;
import webengineering.framework.data.DataException;
import webengineering.nuovissimosoccorsoweb.model.RichiestaSoccorso;
import webengineering.nuovissimosoccorsoweb.model.InfoMissione;

public class AdminDashboardController extends SoccorsoBaseController {
    
    private static final Logger logger = Logger.getLogger(AdminDashboardController.class.getName());
    
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
            
            // Carica SOLO le statistiche - nessun link cliccabile
            DashboardData dashboardData = loadDashboardData(dataLayer);
            
            // Mostra la dashboard
            showDashboard(request, response, dashboardData);
            
        } catch (DataException ex) {
            logger.log(Level.SEVERE, "Errore database nella dashboard admin", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nel caricamento dei dati");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico nella dashboard admin", ex);
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
    
    // Carica SOLO i dati necessari per le statistiche
    private DashboardData loadDashboardData(SoccorsoDataLayer dataLayer) throws DataException {
        DashboardData data = new DashboardData();
        
        // Carica richieste per stato
        data.richiesteConvalidate = dataLayer.getRichiestaSoccorsoDAO().getRichiesteByStato("Convalidata");
        data.richiesteAttive = dataLayer.getRichiestaSoccorsoDAO().getRichiesteByStato("Attiva");
        data.richiesteAnnullate = dataLayer.getRichiestaSoccorsoDAO().getRichiesteByStato("Annullata");
        
        // NUOVO: Conta le missioni concluse (info_missione)
        data.countConclusse = countInfoMissioni(dataLayer);
        
        // Calcola statistiche totali
        data.totaleRichieste = dataLayer.getRichiestaSoccorsoDAO().getAllRichieste().size();
        
        return data;
    }
    
    // Metodo per contare le info_missione (missioni concluse)
    private int countInfoMissioni(SoccorsoDataLayer dataLayer) throws DataException {
        try {
            // Per ora uso una query per ottenere tutte le info_missione e conto
            // Se hai un metodo specifico per il count, usalo
            List<InfoMissione> allInfoMissioni = dataLayer.getInfoMissioneDAO().getAllInfoMissioni();
            return allInfoMissioni.size();
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Errore nel conteggio info_missione, usando count da richieste chiuse", ex);
            // Fallback: conta le richieste con stato "Chiusa"
            List<RichiestaSoccorso> richiesteChiuse = dataLayer.getRichiestaSoccorsoDAO().getRichiesteByStato("Chiusa");
            return richiesteChiuse.size();
        }
    }
    
    // Mostra la dashboard SENZA link cliccabili
    private void showDashboard(HttpServletRequest request, HttpServletResponse response, 
                              DashboardData dashboardData) throws ServletException {
        
        try {
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("thispageurl", request.getAttribute("thispageurl"));
            dataModel.put("outline_tpl", null);
            
            addUserInfoToModel(request, dataModel);
            
            // Aggiungi context path
            dataModel.put("contextPath", request.getContextPath());
            
            // Gestisci messaggi
            String successParam = request.getParameter("success");
            String errorParam = request.getParameter("error");
            
            if (successParam != null && !successParam.trim().isEmpty()) {
                dataModel.put("success_message", successParam);
            }
            
            if (errorParam != null && !errorParam.trim().isEmpty()) {
                dataModel.put("error_message", getErrorMessage(errorParam));
            }
            
            // CONTATORI AGGIORNATI
            dataModel.put("count_convalidate", dashboardData.richiesteConvalidate.size());
            dataModel.put("count_attive", dashboardData.richiesteAttive.size());
            dataModel.put("count_concluse", dashboardData.countConclusse); // NUOVO: da info_missione
            dataModel.put("count_annullate", dashboardData.richiesteAnnullate.size());
            dataModel.put("totale_richieste", dashboardData.totaleRichieste);
            
            // Nome admin
            HttpSession session = request.getSession(false);
            if (session != null) {
                String fullName = (String) session.getAttribute("full_name");
                dataModel.put("admin_name", fullName != null ? fullName : "Amministratore");
            }
            
            new webengineering.framework.result.TemplateResult(getServletContext())
                    .activate("admin_dashboard_simple.ftl.html", dataModel, response);
                    
        } catch (TemplateManagerException ex) {
            logger.log(Level.SEVERE, "Errore nel template della dashboard", ex);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nel caricamento della dashboard");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore critico", e);
            }
        }
    }
    
    // Messaggi di errore
    private String getErrorMessage(String errorCode) {
        switch (errorCode) {
            case "access_denied":
                return "Accesso negato. Solo gli amministratori possono accedere.";
            case "database_error":
                return "Errore nel database. Riprova più tardi.";
            case "system_error":
                return "Errore di sistema. Riprova più tardi.";
            default:
                return "Si è verificato un errore.";
        }
    }
    
    // Classe di supporto AGGIORNATA
    private static class DashboardData {
        List<RichiestaSoccorso> richiesteConvalidate;
        List<RichiestaSoccorso> richiesteAttive;
        List<RichiestaSoccorso> richiesteAnnullate;
        int countConclusse; // NUOVO: conteggio da info_missione
        int totaleRichieste;
    }
}