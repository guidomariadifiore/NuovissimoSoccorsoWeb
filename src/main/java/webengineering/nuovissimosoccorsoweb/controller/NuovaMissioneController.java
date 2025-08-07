package webengineering.nuovissimosoccorsoweb.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.LocalDateTime;
import java.sql.Connection;
import java.sql.SQLException;

import webengineering.framework.result.TemplateManagerException;
import webengineering.nuovissimosoccorsoweb.SoccorsoDataLayer;
import webengineering.framework.data.DataException;
import webengineering.nuovissimosoccorsoweb.model.RichiestaSoccorso;
import webengineering.nuovissimosoccorsoweb.model.Operatore;
import webengineering.nuovissimosoccorsoweb.model.Mezzo;
import webengineering.nuovissimosoccorsoweb.model.Materiale;
import webengineering.nuovissimosoccorsoweb.model.Missione;
import webengineering.nuovissimosoccorsoweb.model.TipoPatente;
import webengineering.nuovissimosoccorsoweb.model.TipoAbilita;
import webengineering.nuovissimosoccorsoweb.model.impl.MissioneImpl;

public class NuovaMissioneController extends SoccorsoBaseController {
    
    private static final Logger logger = Logger.getLogger(NuovaMissioneController.class.getName());
    
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
            // Gestisce creazione missione
            handleCreateMission(request, response);
        } else {
            // Mostra form per creare missione
            showMissionForm(request, response);
        }
    }
    
    private boolean isUserAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        
        String userRole = (String) session.getAttribute("user_role");
        return "admin".equals(userRole);
    }
    
    private void showMissionForm(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        try {
            // Estrai ID richiesta dalla URL
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
                logger.warning("ID richiesta non valido: " + idString);
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
            
            // Verifica che sia stata attivata
            if (!"Attiva".equals(richiesta.getStato())) {
                logger.warning("Tentativo di creare missione per richiesta non attiva: " + richiestaId);
                response.sendRedirect(request.getContextPath() + "/admin/richiesta/" + richiestaId + 
                                    "?error=not_active");
                return;
            }
            
            // Carica risorse disponibili
            List<Operatore> operatoriDisponibili = dataLayer.getOperatoreDAO().getOperatoriDisponibili();
            List<Mezzo> mezziDisponibili = dataLayer.getMezzoDAO().getMezziDisponibili();
            List<Materiale> materialiDisponibili = dataLayer.getMaterialeDAO().getMaterialiDisponibili();
            
            // Arricchisci operatori con patenti e abilità
            Map<String, List<String>> operatoriPatenti = new HashMap<>();
            Map<String, List<String>> operatoriAbilita = new HashMap<>();
            
            for (Operatore operatore : operatoriDisponibili) {
                try {
                    // Usa String come chiave invece di Integer per compatibilità FreeMarker
                    String operatoreKey = String.valueOf(operatore.getId());
                    
                    // Carica patenti
                    List<String> patenti = dataLayer.getOperatoreHaPatenteDAO()
                        .getPatentiByOperatore(operatore.getId())
                        .stream()
                        .map(TipoPatente::toDBString)
                        .collect(java.util.stream.Collectors.toList());
                    operatoriPatenti.put(operatoreKey, patenti);
                    
                    // Carica abilità
                    List<String> abilita = dataLayer.getOperatoreHaAbilitaDAO()
                        .getAbilitaByOperatore(operatore.getId())
                        .stream()
                        .map(a -> a.getTipo().toDBString())
                        .collect(java.util.stream.Collectors.toList());
                    operatoriAbilita.put(operatoreKey, abilita);
                    
                } catch (DataException ex) {
                    logger.log(Level.WARNING, "Errore caricamento patenti/abilità per operatore " + operatore.getId(), ex);
                    // Continua con liste vuote
                    String operatoreKey = String.valueOf(operatore.getId());
                    operatoriPatenti.put(operatoreKey, new ArrayList<>());
                    operatoriAbilita.put(operatoreKey, new ArrayList<>());
                }
            }
            
            logger.info("Risorse caricate - Operatori: " + operatoriDisponibili.size() + 
                       ", Mezzi: " + mezziDisponibili.size() + 
                       ", Materiali: " + materialiDisponibili.size());
            
            // Mostra form
            displayMissionForm(request, response, richiesta, operatoriDisponibili, mezziDisponibili, materialiDisponibili, 
                             operatoriPatenti, operatoriAbilita);
            
        } catch (DataException ex) {
            logger.log(Level.SEVERE, "Errore database", ex);
            showErrorPage(request, response, "Errore caricamento dati");
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico", ex);
            showErrorPage(request, response, "Errore di sistema");
        }
    }
    
    private void displayMissionForm(HttpServletRequest request, HttpServletResponse response, 
                                   RichiestaSoccorso richiesta,
                                   List<Operatore> operatoriDisponibili,
                                   List<Mezzo> mezziDisponibili,
                                   List<Materiale> materialiDisponibili,
                                   Map<String, List<String>> operatoriPatenti,
                                   Map<String, List<String>> operatoriAbilita) throws ServletException {
        
        try {
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("thispageurl", request.getAttribute("thispageurl"));
            dataModel.put("outline_tpl", null);
            
            addUserInfoToModel(request, dataModel);
            dataModel.put("contextPath", request.getContextPath());
            
            // Dati della richiesta originale
            dataModel.put("richiesta", richiesta);
            
            // Valori pre-compilati per la missione (tutti modificabili)
            dataModel.put("nome_missione", richiesta.getNome()); // Copiato da richiesta ma modificabile
            dataModel.put("posizione_missione", richiesta.getIndirizzo()); // Copiato da richiesta ma modificabile
            dataModel.put("obiettivo_missione", ""); // Campo vuoto da compilare
            
            // Risorse disponibili
            dataModel.put("operatori_disponibili", operatoriDisponibili);
            dataModel.put("mezzi_disponibili", mezziDisponibili);
            dataModel.put("materiali_disponibili", materialiDisponibili);
            
            // Patenti e abilità operatori
            dataModel.put("operatori_patenti", operatoriPatenti);
            dataModel.put("operatori_abilita", operatoriAbilita);
            
            // Info pagina
            dataModel.put("page_title", "Crea Nuova Missione");
            dataModel.put("page_icon", "🎯");
            
            // Gestisci messaggi di errore se presenti
            String errorParam = request.getParameter("error");
            if (errorParam != null && !errorParam.trim().isEmpty()) {
                dataModel.put("error_message", getErrorMessage(errorParam));
            }
            
            new webengineering.framework.result.TemplateResult(getServletContext())
                    .activate("nuova_missione_form.ftl.html", dataModel, response);
                    
        } catch (TemplateManagerException ex) {
            logger.log(Level.SEVERE, "Errore template", ex);
            handleError(ex, request, response);
        }
    }
    
    private void handleCreateMission(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        try {
            // Estrai ID richiesta dalla URL
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || pathInfo.length() <= 1) {
                response.sendRedirect(request.getContextPath() + "/admin/dashboard?error=invalid_request");
                return;
            }
            
            String idString = pathInfo.substring(1);
            int richiestaId;
            
            try {
                richiestaId = Integer.parseInt(idString);
            } catch (NumberFormatException ex) {
                logger.warning("ID richiesta non valido per POST: " + idString);
                response.sendRedirect(request.getContextPath() + "/admin/dashboard?error=invalid_id");
                return;
            }
            
            // Ottieni DataLayer
            SoccorsoDataLayer dataLayer = (SoccorsoDataLayer) request.getAttribute("datalayer");
            if (dataLayer == null) {
                logger.severe("DataLayer non disponibile per creazione missione");
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore sistema");
                return;
            }
            
            // Verifica che la richiesta esista e sia attiva
            RichiestaSoccorso richiesta = dataLayer.getRichiestaSoccorsoDAO().getRichiestaByCodice(richiestaId);
            if (richiesta == null) {
                logger.warning("Richiesta non trovata per creazione missione: " + richiestaId);
                response.sendRedirect(request.getContextPath() + "/admin/dashboard?error=not_found");
                return;
            }
            
            if (!"Attiva".equals(richiesta.getStato())) {
                logger.warning("Tentativo di creare missione per richiesta non attiva: " + richiestaId);
                response.sendRedirect(request.getContextPath() + "/admin/richiesta/" + richiestaId + 
                                    "?error=not_active");
                return;
            }
            
            // Estrai e valida i parametri del form
            String nome = request.getParameter("nome");
            String posizione = request.getParameter("posizione");
            String obiettivo = request.getParameter("obiettivo");
            String[] operatoriSelezionati = request.getParameterValues("selected_operatori");
            String[] mezziSelezionati = request.getParameterValues("selected_mezzi");
            String[] materialiSelezionati = request.getParameterValues("selected_materiali");
            
            // Validazione parametri
            if (nome == null || nome.trim().isEmpty()) {
                redirectWithError(request, response, richiestaId, "Nome missione obbligatorio");
                return;
            }
            
            if (posizione == null || posizione.trim().isEmpty()) {
                redirectWithError(request, response, richiestaId, "Posizione missione obbligatoria");
                return;
            }
            
            if (obiettivo == null || obiettivo.trim().isEmpty()) {
                redirectWithError(request, response, richiestaId, "Obiettivo missione obbligatorio");
                return;
            }
            
            if (operatoriSelezionati == null || operatoriSelezionati.length == 0) {
                redirectWithError(request, response, richiestaId, "Almeno un operatore è richiesto");
                return;
            }
            
            // Validazione capo squadra
            boolean hasCaposquadra = false;
            for (String operatoreId : operatoriSelezionati) {
                String ruolo = request.getParameter("ruolo_operatore_" + operatoreId);
                if ("Caposquadra".equals(ruolo)) {
                    hasCaposquadra = true;
                    break;
                }
            }
            
            if (!hasCaposquadra) {
                redirectWithError(request, response, richiestaId, "La squadra deve avere almeno un Capo Squadra");
                return;
            }
            
            // Procedi con la creazione della missione
            createMissionWithResources(dataLayer, richiestaId, nome.trim(), posizione.trim(), obiettivo.trim(),
                                     operatoriSelezionati, mezziSelezionati, materialiSelezionati, request, response);
            
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore durante creazione missione", ex);
            handleError(ex, request, response);
        }
    }
    
    private void createMissionWithResources(SoccorsoDataLayer dataLayer, int richiestaId, 
                                          String nome, String posizione, String obiettivo,
                                          String[] operatori, String[] mezzi, String[] materiali,
                                          HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        try {
            // Avvia transazione
            Connection conn = dataLayer.getConnection();
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            
            try {
                // 1. Crea la missione
                Missione missione = new MissioneImpl();
                missione.setCodiceRichiesta(richiestaId);
                missione.setNome(nome);
                missione.setPosizione(posizione);
                missione.setObiettivo(obiettivo);
                missione.setNota(""); // Nota vuota per ora
                missione.setDataOraInizio(LocalDateTime.now());
                missione.setVersion(1);
                
                // Ottieni ID amministratore dalla sessione
                HttpSession session = request.getSession(false);
                Integer adminId = null;
                
                // Usa "userid" come nel Login.java (SecurityHelpers.createSession)
                if (session != null) {
                    adminId = (Integer) session.getAttribute("userid");
                    
                    // Debug: logga l'ID trovato
                    logger.info("ID amministratore dalla sessione (userid): " + adminId);
                }
                
                if (adminId == null) {
                    throw new DataException("ID amministratore non trovato in sessione");
                }
                missione.setIdAmministratore(adminId);
                
                // Salva la missione
                dataLayer.getMissioneDAO().storeMissione(missione);
                logger.info("Missione creata con successo per richiesta " + richiestaId);
                
                // 2. Assegna operatori alla squadra
                if (operatori != null && operatori.length > 0) {
                    for (String operatoreIdStr : operatori) {
                        try {
                            int operatoreId = Integer.parseInt(operatoreIdStr);
                            String ruolo = request.getParameter("ruolo_operatore_" + operatoreId);
                            
                            if (ruolo == null || ruolo.trim().isEmpty()) {
                                ruolo = "Standard"; // Default
                            }
                            
                            dataLayer.getMissioneDAO().assegnaOperatoreAMissione(operatoreId, richiestaId, ruolo);
                            logger.info("Operatore " + operatoreId + " assegnato con ruolo " + ruolo);
                            
                        } catch (NumberFormatException ex) {
                            logger.warning("ID operatore non valido: " + operatoreIdStr);
                        }
                    }
                }
                
                // 3. Assegna mezzi alla missione
                if (mezzi != null && mezzi.length > 0) {
                    for (String targa : mezzi) {
                        if (targa != null && !targa.trim().isEmpty()) {
                            dataLayer.getMissioneDAO().assegnaMezzoAMissione(targa.trim(), richiestaId);
                            logger.info("Mezzo " + targa + " assegnato alla missione");
                        }
                    }
                }
                
                // 4. Assegna materiali alla missione
                if (materiali != null && materiali.length > 0) {
                    for (String materialeIdStr : materiali) {
                        try {
                            int materialeId = Integer.parseInt(materialeIdStr);
                            dataLayer.getMissioneDAO().assegnaMaterialeAMissione(materialeId, richiestaId);
                            logger.info("Materiale " + materialeId + " assegnato alla missione");
                            
                        } catch (NumberFormatException ex) {
                            logger.warning("ID materiale non valido: " + materialeIdStr);
                        }
                    }
                }
                
                // Commit della transazione
                conn.commit();
                logger.info("Missione completa creata con successo per richiesta " + richiestaId);
                
                // NUOVO: Raccogli le email degli operatori per la notifica
                List<String> emailOperatori = new ArrayList<>();
                if (operatori != null && operatori.length > 0) {
                    for (String operatoreIdStr : operatori) {
                        try {
                            int operatoreId = Integer.parseInt(operatoreIdStr);
                            Operatore operatore = dataLayer.getOperatoreDAO().getOperatoreById(operatoreId);
                            if (operatore != null && operatore.getEmail() != null && !operatore.getEmail().trim().isEmpty()) {
                                emailOperatori.add(operatore.getEmail());
                                logger.info("Email simulata inviata a: " + operatore.getEmail());
                            }
                        } catch (NumberFormatException ex) {
                            logger.warning("ID operatore non valido per email: " + operatoreIdStr);
                        } catch (DataException ex) {
                            logger.warning("Errore caricamento operatore per email: " + operatoreIdStr);
                        }
                    }
                }
                
                // Log operazione completa
                String adminName = session != null ? (String) session.getAttribute("full_name") : "Admin";
                logger.info("Admin " + adminName + " ha creato missione '" + nome + "' per richiesta " + richiestaId + 
                          " con " + (operatori != null ? operatori.length : 0) + " operatori, " + 
                          (mezzi != null ? mezzi.length : 0) + " mezzi, " + 
                          (materiali != null ? materiali.length : 0) + " materiali");
                
                if (!emailOperatori.isEmpty()) {
                    logger.info("Email simulate inviate a: " + String.join(", ", emailOperatori));
                }
                
                // Mostra pagina di successo con email invece del redirect diretto
                showMissionSuccessPage(request, response, nome, emailOperatori);
                
            } catch (Exception ex) {
                // Rollback in caso di errore
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    logger.log(Level.SEVERE, "Errore durante rollback", rollbackEx);
                }
                throw ex;
            } finally {
                // Ripristina auto-commit
                try {
                    conn.setAutoCommit(autoCommit);
                } catch (SQLException ex) {
                    logger.log(Level.WARNING, "Errore ripristino auto-commit", ex);
                }
            }
            
        } catch (DataException ex) {
            logger.log(Level.SEVERE, "Errore database durante creazione missione", ex);
            redirectWithError(request, response, richiestaId, "Errore database: " + ex.getMessage());
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico durante creazione missione", ex);
            redirectWithError(request, response, richiestaId, "Errore durante la creazione della missione");
        }
    }
    
    // NUOVO METODO: Mostra pagina di successo con notifica email
    private void showMissionSuccessPage(HttpServletRequest request, HttpServletResponse response, 
                                       String nomeMissione, List<String> emailOperatori) throws ServletException {
        try {
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("thispageurl", request.getAttribute("thispageurl"));
            dataModel.put("outline_tpl", null);
            
            addUserInfoToModel(request, dataModel);
            dataModel.put("contextPath", request.getContextPath());
            
            // Dati della missione creata
            dataModel.put("mission_name", nomeMissione);
            dataModel.put("emails_sent", emailOperatori);
            dataModel.put("success_message", "Missione '" + nomeMissione + "' creata con successo!");
            
            // Info per il template
            dataModel.put("page_title", "Missione Creata con Successo");
            dataModel.put("page_icon", "✅");
            
            logger.info("Mostrando pagina di successo per missione: " + nomeMissione + 
                       " con " + emailOperatori.size() + " email simulate");
            
            new webengineering.framework.result.TemplateResult(getServletContext())
                    .activate("mission_success.ftl.html", dataModel, response);
                    
        } catch (TemplateManagerException ex) {
            logger.log(Level.SEVERE, "Errore template successo missione", ex);
            // Fallback: redirect normale al dashboard
            try {
                response.sendRedirect(request.getContextPath() + "/admin/dashboard" + 
                                    "?success=" + java.net.URLEncoder.encode("Missione '" + nomeMissione + "' creata con successo!", "UTF-8"));
            } catch (Exception redirectEx) {
                logger.log(Level.SEVERE, "Errore anche nel fallback redirect", redirectEx);
                handleError(redirectEx, request, response);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico in showMissionSuccessPage", ex);
            // Fallback: redirect normale al dashboard
            try {
                response.sendRedirect(request.getContextPath() + "/admin/dashboard" + 
                                    "?success=" + java.net.URLEncoder.encode("Missione '" + nomeMissione + "' creata con successo!", "UTF-8"));
            } catch (Exception redirectEx) {
                logger.log(Level.SEVERE, "Errore anche nel fallback redirect", redirectEx);
                handleError(redirectEx, request, response);
            }
        }
    }
    
    private void redirectWithError(HttpServletRequest request, HttpServletResponse response, 
                                 int richiestaId, String errorMessage) throws ServletException {
        try {
            String encodedError = java.net.URLEncoder.encode(errorMessage, "UTF-8");
            response.sendRedirect(request.getContextPath() + "/admin/NuovaMissione/" + richiestaId + 
                                "?error=" + encodedError);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore redirect con errore", ex);
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
                return "Accesso negato. Solo gli amministratori possono creare missioni.";
            case "invalid_id":
                return "ID richiesta non valido.";
            case "not_found":
                return "Richiesta non trovata.";
            case "not_active":
                return "La richiesta deve essere in stato 'Attiva' per creare una missione.";
            case "database_error":
                return "Errore nel database. Riprova più tardi.";
            case "system_error":
                return "Errore di sistema. Riprova più tardi.";
            default:
                return "Si è verificato un errore: " + errorCode;
        }
    }
}