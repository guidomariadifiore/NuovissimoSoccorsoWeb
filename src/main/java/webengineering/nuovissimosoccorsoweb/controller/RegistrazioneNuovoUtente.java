package webengineering.nuovissimosoccorsoweb.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import webengineering.framework.result.TemplateManagerException;
import webengineering.framework.security.SecurityHelpers;
import webengineering.nuovissimosoccorsoweb.SoccorsoDataLayer;
import webengineering.framework.data.DataException;
import webengineering.nuovissimosoccorsoweb.model.Amministratore;
import webengineering.nuovissimosoccorsoweb.model.Operatore;
import webengineering.nuovissimosoccorsoweb.model.TipoPatente;
import webengineering.nuovissimosoccorsoweb.model.TipoAbilita;
import webengineering.nuovissimosoccorsoweb.model.impl.AmministratoreImpl;
import webengineering.nuovissimosoccorsoweb.model.impl.OperatoreImpl;

public class RegistrazioneNuovoUtente extends SoccorsoBaseController {
    
    private static final Logger logger = Logger.getLogger(RegistrazioneNuovoUtente.class.getName());
    
    // Pattern per validazione Codice Fiscale italiano
    private static final Pattern CF_PATTERN = Pattern.compile("^[A-Z]{6}[0-9]{2}[A-Z][0-9]{2}[A-Z][0-9]{3}[A-Z]$");
    
    // Pattern per validazione email
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    
    // Mostra il form di registrazione (GET)
    private void showRegistrationForm(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        // Verifica che l'utente sia un amministratore
        if (!isUserAdministrator(request)) {
            try {
                String contextPath = request.getContextPath();
                response.sendRedirect(contextPath + "/login?error=access_denied");
                return;
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Errore durante redirect", ex);
                return;
            }
        }
        
        try {
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("thispageurl", request.getAttribute("thispageurl"));
            dataModel.put("outline_tpl", null);
            
            // Aggiungi informazioni utente al dataModel
            addUserInfoToModel(request, dataModel);
            
            // Messaggi di errore o successo
            String error = request.getParameter("error");
            String success = request.getParameter("success");
            
            if (error != null) {
                dataModel.put("error_message", getErrorMessage(error));
            }
            if (success != null) {
                dataModel.put("success_message", getSuccessMessage(success));
            }
            
            // Informazioni dell'amministratore corrente
            Amministratore currentAdmin = getCurrentAdministrator(request);
            if (currentAdmin != null) {
                dataModel.put("current_admin_name", currentAdmin.getNome() + " " + currentAdmin.getCognome());
            }
            
            new webengineering.framework.result.TemplateResult(getServletContext())
                    .activate("registrazione_nuovo_utente.ftl.html", dataModel, response);
                    
        } catch (TemplateManagerException ex) {
            logger.log(Level.SEVERE, "Errore nel template di registrazione", ex);
            handleError(ex, request, response);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico nella registrazione", ex);
            handleError(ex, request, response);
        }
    }
    
    // Gestisce la registrazione del nuovo utente (POST)
    private void processUserRegistration(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, Exception {
        
        // Verifica che l'utente sia un amministratore
        if (!isUserAdministrator(request)) {
            redirectWithError(response, "access_denied", request);
            return;
        }
        
        try {
            // Recupera e sanifica i parametri dal form
            String tipoUtente = request.getParameter("tipo_utente"); // "amministratore" o "operatore"
            String nome = request.getParameter("nome");
            String cognome = request.getParameter("cognome");
            String codiceFiscale = request.getParameter("codice_fiscale");
            String email = request.getParameter("email");
            String password = request.getParameter("password");
            String confermaPassword = request.getParameter("conferma_password");
            
            // Recupera patenti e abilità (solo per operatori)
            String[] patentiSelezionate = request.getParameterValues("patenti");
            String[] abilitaSelezionate = request.getParameterValues("abilita");
            
            // Sanificazione dati base
            if (tipoUtente != null) tipoUtente = SecurityHelpers.addSlashes(tipoUtente.trim());
            if (nome != null) nome = SecurityHelpers.addSlashes(nome.trim());
            if (cognome != null) cognome = SecurityHelpers.addSlashes(cognome.trim());
            if (codiceFiscale != null) codiceFiscale = SecurityHelpers.addSlashes(codiceFiscale.trim().toUpperCase());
            if (email != null) email = SecurityHelpers.addSlashes(email.trim().toLowerCase());
            if (password != null) password = password.trim(); // Non sanificare la password
            if (confermaPassword != null) confermaPassword = confermaPassword.trim();
            
            // Validazione input
            if (!validateInput(tipoUtente, nome, cognome, codiceFiscale, email, password, confermaPassword, request, response)) {
                return;
            }
            
            // Ottieni il DataLayer
            SoccorsoDataLayer dataLayer = (SoccorsoDataLayer) request.getAttribute("datalayer");
            if (dataLayer == null) {
                logger.severe("DataLayer non disponibile nella request");
                redirectWithError(response, "system_error", request);
                return;
            }
            
            // Ottieni l'amministratore corrente (creatore)
            Amministratore currentAdmin = getCurrentAdministrator(request);
            if (currentAdmin == null) {
                logger.severe("Amministratore corrente non trovato");
                redirectWithError(response, "session_error", request);
                return;
            }
            
            // Verifica che email e CF non esistano già
            if (isEmailOrCfAlreadyExists(dataLayer, email, codiceFiscale, request, response)) {
                return;
            }
            
            // Hash della password
            String hashedPassword = hashPassword(password);
            if (hashedPassword == null) {
                redirectWithError(response, "password_hash_error", request);
                return;
            }
            
            // Variabili per la pagina di successo
            String nomeCompleto = nome + " " + cognome;
            List<String> emailInviate = new ArrayList<>();
            emailInviate.add(email); // L'email del nuovo utente
            
            // Crea e salva l'utente in base al tipo
            if ("amministratore".equals(tipoUtente)) {
                createAdministrator(dataLayer, nome, cognome, codiceFiscale, email, hashedPassword, currentAdmin.getId());
                logger.info("Nuovo amministratore creato: " + nome + " " + cognome + " (" + email + ") da admin ID " + currentAdmin.getId());
                
                // Simula invio email di benvenuto per amministratore
                simulateWelcomeEmail(email, nomeCompleto, tipoUtente, null, password);
                
                // Mostra pagina di successo
                showUserCreationSuccessPage(request, response, nomeCompleto, "Amministratore", emailInviate, null, null);
                
            } else if ("operatore".equals(tipoUtente)) {
                int operatoreId = createOperator(dataLayer, nome, cognome, codiceFiscale, email, hashedPassword, currentAdmin.getId());
                
                // Liste per patenti e abilità assegnate
                List<String> patentiAssegnate = new ArrayList<>();
                List<String> abilitaAssegnate = new ArrayList<>();
                
                // Aggiungi patenti e abilità se l'operatore è stato creato con successo
                if (operatoreId > 0) {
                    patentiAssegnate = assignPatentiToOperatore(dataLayer, operatoreId, patentiSelezionate);
                    abilitaAssegnate = assignAbilitaToOperatore(dataLayer, operatoreId, abilitaSelezionate);
                }
                
                logger.info("Nuovo operatore creato: " + nome + " " + cognome + " (" + email + ") con " + 
                           patentiAssegnate.size() + " patenti e " + abilitaAssegnate.size() + " abilità da admin ID " + currentAdmin.getId());
                
                // Simula invio email di benvenuto per operatore
                simulateWelcomeEmail(email, nomeCompleto, tipoUtente, patentiAssegnate, abilitaAssegnate, password);
                
                // Mostra pagina di successo
                showUserCreationSuccessPage(request, response, nomeCompleto, "Operatore", emailInviate, patentiAssegnate, abilitaAssegnate);
                
            } else {
                redirectWithError(response, "tipo_utente_invalid", request);
                return;
            }
            
        } catch (DataException ex) {
            logger.log(Level.SEVERE, "Errore database durante registrazione utente", ex);
            redirectWithError(response, "database_error", request);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico durante registrazione utente", ex);
            redirectWithError(response, "system_error", request);
        }
    }
    
    // NUOVO METODO: Simula l'invio dell'email di benvenuto
    private void simulateWelcomeEmail(String email, String nomeCompleto, String tipoUtente, 
                                     List<String> patenti, String password) {
        logger.info("Simulazione invio email di benvenuto a: " + email);
        logger.info("Destinatario: " + nomeCompleto + " (Nuovo " + tipoUtente + ")");
        logger.info("Contenuto email simulato:");
        logger.info("  - Credenziali di accesso fornite");
        logger.info("  - Password temporanea: [NASCOSTA PER SICUREZZA]");
        if (patenti != null && !patenti.isEmpty()) {
            logger.info("  - Patenti assegnate: " + String.join(", ", patenti));
        }
    }
    
    // Overload del metodo per operatori con abilità
    private void simulateWelcomeEmail(String email, String nomeCompleto, String tipoUtente, 
                                     List<String> patenti, List<String> abilita, String password) {
        logger.info("Simulazione invio email di benvenuto a: " + email);
        logger.info("Destinatario: " + nomeCompleto + " (Nuovo " + tipoUtente + ")");
        logger.info("Contenuto email simulato:");
        logger.info("  - Credenziali di accesso fornite");
        logger.info("  - Password temporanea: [NASCOSTA PER SICUREZZA]");
        if (patenti != null && !patenti.isEmpty()) {
            logger.info("  - Patenti assegnate: " + String.join(", ", patenti));
        }
        if (abilita != null && !abilita.isEmpty()) {
            logger.info("  - Abilità assegnate: " + String.join(", ", abilita));
        }
    }
    
    // NUOVO METODO: Mostra pagina di successo con notifica email
    private void showUserCreationSuccessPage(HttpServletRequest request, HttpServletResponse response, 
                                           String nomeUtente, String tipoUtente, List<String> emailInviate,
                                           List<String> patenti, List<String> abilita) throws ServletException {
        try {
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("thispageurl", request.getAttribute("thispageurl"));
            dataModel.put("outline_tpl", null);
            
            addUserInfoToModel(request, dataModel);
            dataModel.put("contextPath", request.getContextPath());
            
            // Dati dell'utente creato
            dataModel.put("user_name", nomeUtente);
            dataModel.put("user_type", tipoUtente);
            dataModel.put("emails_sent", emailInviate);
            dataModel.put("success_message", tipoUtente + " '" + nomeUtente + "' creato con successo!");
            
            // Dati specifici per operatori
            if (patenti != null) {
                dataModel.put("assigned_patenti", patenti);
            }
            if (abilita != null) {
                dataModel.put("assigned_abilita", abilita);
            }
            
            // Info per il template
            dataModel.put("page_title", "Utente Creato con Successo");
            dataModel.put("page_icon", "👥");
            
            logger.info("Mostrando pagina di successo per utente: " + nomeUtente + " (" + tipoUtente + ") con email simulate");
            
            new webengineering.framework.result.TemplateResult(getServletContext())
                    .activate("user_creation_success.ftl.html", dataModel, response);
                    
        } catch (TemplateManagerException ex) {
            logger.log(Level.SEVERE, "Errore template successo creazione utente", ex);
            // Fallback: redirect normale alla registrazione
            try {
                response.sendRedirect(request.getContextPath() + "/admin/registrazione" + 
                                    "?success=" + java.net.URLEncoder.encode(tipoUtente.toLowerCase() + "_created", "UTF-8"));
            } catch (Exception redirectEx) {
                logger.log(Level.SEVERE, "Errore anche nel fallback redirect", redirectEx);
                handleError(redirectEx, request, response);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico in showUserCreationSuccessPage", ex);
            // Fallback: redirect normale alla registrazione
            try {
                response.sendRedirect(request.getContextPath() + "/admin/registrazione" + 
                                    "?success=" + java.net.URLEncoder.encode(tipoUtente.toLowerCase() + "_created", "UTF-8"));
            } catch (Exception redirectEx) {
                logger.log(Level.SEVERE, "Errore anche nel fallback redirect", redirectEx);
                handleError(redirectEx, request, response);
            }
        }
    }
    
    // Assegna le patenti selezionate all'operatore - MODIFICATO per restituire la lista
    private List<String> assignPatentiToOperatore(SoccorsoDataLayer dataLayer, int operatoreId, String[] patentiSelezionate) {
        List<String> patentiAssegnate = new ArrayList<>();
        
        if (patentiSelezionate == null || patentiSelezionate.length == 0) {
            logger.info("Nessuna patente selezionata per operatore ID: " + operatoreId);
            return patentiAssegnate;
        }
        
        try {
            for (String patenteStr : patentiSelezionate) {
                try {
                    // Converti la stringa nel TipoPatente enum
                    TipoPatente tipoPatente = TipoPatente.fromString(patenteStr);
                    dataLayer.getOperatoreHaPatenteDAO().assegnaPatente(operatoreId, tipoPatente);
                    patentiAssegnate.add(patenteStr);
                    logger.info("Patente " + patenteStr + " assegnata all'operatore ID: " + operatoreId);
                } catch (IllegalArgumentException ex) {
                    logger.warning("Tipo patente non valido: " + patenteStr + " per operatore ID: " + operatoreId);
                } catch (DataException ex) {
                    logger.log(Level.WARNING, "Errore nell'assegnazione patente " + patenteStr + " all'operatore ID: " + operatoreId, ex);
                }
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico nell'assegnazione patenti all'operatore ID: " + operatoreId, ex);
        }
        
        return patentiAssegnate;
    }
    
    // Assegna le abilità selezionate all'operatore - MODIFICATO per restituire la lista
    private List<String> assignAbilitaToOperatore(SoccorsoDataLayer dataLayer, int operatoreId, String[] abilitaSelezionate) {
        List<String> abilitaAssegnate = new ArrayList<>();
        
        if (abilitaSelezionate == null || abilitaSelezionate.length == 0) {
            logger.info("Nessuna abilità selezionata per operatore ID: " + operatoreId);
            return abilitaAssegnate;
        }
        
        try {
            for (String abilitaStr : abilitaSelezionate) {
                try {
                    // Converti la stringa nel TipoAbilita enum
                    TipoAbilita tipoAbilita = TipoAbilita.fromString(abilitaStr);
                    
                    // Ottieni l'ID dell'abilità
                    int abilitaId = getAbilitaIdByTipo(dataLayer, tipoAbilita);
                    if (abilitaId > 0) {
                        dataLayer.getOperatoreHaAbilitaDAO().assegnaAbilita(operatoreId, abilitaId);
                        abilitaAssegnate.add(abilitaStr);
                        logger.info("Abilità " + abilitaStr + " (ID: " + abilitaId + ") assegnata all'operatore ID: " + operatoreId);
                    } else {
                        logger.warning("Abilità non trovata nel database: " + abilitaStr);
                    }
                    
                } catch (IllegalArgumentException ex) {
                    logger.warning("Tipo abilità non valido: " + abilitaStr + " per operatore ID: " + operatoreId);
                } catch (DataException ex) {
                    logger.log(Level.WARNING, "Errore nell'assegnazione abilità " + abilitaStr + " all'operatore ID: " + operatoreId, ex);
                }
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico nell'assegnazione abilità all'operatore ID: " + operatoreId, ex);
        }
        
        return abilitaAssegnate;
    }
    
    // Helper method per ottenere l'ID dell'abilità dal tipo
    private int getAbilitaIdByTipo(SoccorsoDataLayer dataLayer, TipoAbilita tipo) throws DataException {
        try {
            // Query diretta per ottenere l'ID
            String query = "SELECT id FROM abilita WHERE tipo = ?";
            try (java.sql.PreparedStatement stmt = dataLayer.getConnection().prepareStatement(query)) {
                stmt.setString(1, tipo.toDBString());
                try (java.sql.ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("id");
                    }
                }
            }
            
            // Se l'abilità non esiste, creala
            return createAbilitaIfNotExists(dataLayer, tipo);
            
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Errore nel recupero ID abilità per tipo: " + tipo, ex);
            return 0;
        }
    }
    
    // Crea un'abilità nel database se non esiste
    private int createAbilitaIfNotExists(SoccorsoDataLayer dataLayer, TipoAbilita tipo) throws DataException {
        try {
            String insertQuery = "INSERT INTO abilita (tipo) VALUES (?)";
            try (java.sql.PreparedStatement stmt = dataLayer.getConnection().prepareStatement(insertQuery, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, tipo.toDBString());
                int affectedRows = stmt.executeUpdate();
                
                if (affectedRows > 0) {
                    try (java.sql.ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            int newId = generatedKeys.getInt(1);
                            logger.info("Creata nuova abilità: " + tipo.toDBString() + " con ID: " + newId);
                            return newId;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Errore nella creazione abilità: " + tipo, ex);
        }
        return 0;
    }
    
    // RESTO DEL CODICE RIMANE INVARIATO...
    
    // Validazione completa dell'input
    private boolean validateInput(String tipoUtente, String nome, String cognome, String codiceFiscale, 
                                 String email, String password, String confermaPassword,
                                 HttpServletRequest request, HttpServletResponse response) throws Exception {
        
        // Tipo utente
        if (tipoUtente == null || (!tipoUtente.equals("amministratore") && !tipoUtente.equals("operatore"))) {
            redirectWithError(response, "tipo_utente_required", request);
            return false;
        }
        
        // Nome
        if (nome == null || nome.isEmpty() || nome.length() < 2) {
            redirectWithError(response, "nome_invalid", request);
            return false;
        }
        
        // Cognome
        if (cognome == null || cognome.isEmpty() || cognome.length() < 2) {
            redirectWithError(response, "cognome_invalid", request);
            return false;
        }
        
        // Codice Fiscale
        if (codiceFiscale == null || !CF_PATTERN.matcher(codiceFiscale).matches()) {
            redirectWithError(response, "cf_invalid", request);
            return false;
        }
        
        // Email
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            redirectWithError(response, "email_invalid", request);
            return false;
        }
        
        // Password
        if (password == null || password.length() < 8) {
            redirectWithError(response, "password_too_short", request);
            return false;
        }
        
        // Conferma password
        if (!password.equals(confermaPassword)) {
            redirectWithError(response, "password_mismatch", request);
            return false;
        }
        
        return true;
    }
    
    // Verifica se email o CF esistono già
    private boolean isEmailOrCfAlreadyExists(SoccorsoDataLayer dataLayer, String email, String codiceFiscale,
                                           HttpServletRequest request, HttpServletResponse response) throws Exception {
        
        try {
            // Controlla negli amministratori
            if (dataLayer.getAmministratoreDAO().getAmministratoreByEmail(email) != null) {
                redirectWithError(response, "email_already_exists", request);
                return true;
            }
            
            if (dataLayer.getAmministratoreDAO().getAmministratoreByCf(codiceFiscale) != null) {
                redirectWithError(response, "cf_already_exists", request);
                return true;
            }
            
            // Controlla negli operatori
            if (dataLayer.getOperatoreDAO().getOperatoreByEmail(email) != null) {
                redirectWithError(response, "email_already_exists", request);
                return true;
            }
            
            if (dataLayer.getOperatoreDAO().getOperatoreByCf(codiceFiscale) != null) {
                redirectWithError(response, "cf_already_exists", request);
                return true;
            }
            
        } catch (DataException ex) {
            logger.log(Level.WARNING, "Errore durante verifica esistenza email/CF", ex);
        }
        
        return false;
    }
    
    // Crea un nuovo amministratore
    private void createAdministrator(SoccorsoDataLayer dataLayer, String nome, String cognome, 
                                   String codiceFiscale, String email, String hashedPassword, int idCreatore) 
                                   throws DataException {
        
        AmministratoreImpl admin = new AmministratoreImpl();
        admin.setNome(SecurityHelpers.stripSlashes(nome));
        admin.setCognome(SecurityHelpers.stripSlashes(cognome));
        admin.setCf(SecurityHelpers.stripSlashes(codiceFiscale));
        admin.setEmail(SecurityHelpers.stripSlashes(email));
        admin.setPassword(hashedPassword);
        admin.setRuolo("amministratore");
        admin.setIdCreatore(idCreatore);
        admin.setVersion(1);
        
        dataLayer.getAmministratoreDAO().storeAmministratore(admin);
    }
    
    // Crea un nuovo operatore e restituisce l'ID
    private int createOperator(SoccorsoDataLayer dataLayer, String nome, String cognome, 
                              String codiceFiscale, String email, String hashedPassword, int idCreatore) 
                              throws DataException {
        
        OperatoreImpl operatore = new OperatoreImpl();
        operatore.setNome(SecurityHelpers.stripSlashes(nome));
        operatore.setCognome(SecurityHelpers.stripSlashes(cognome));
        operatore.setCodiceFiscale(SecurityHelpers.stripSlashes(codiceFiscale));
        operatore.setEmail(SecurityHelpers.stripSlashes(email));
        operatore.setPassword(hashedPassword);
        operatore.setRuolo("operatore");
        operatore.setIdAmministratore(idCreatore);
        operatore.setVersion(1);
        
        dataLayer.getOperatoreDAO().storeOperatore(operatore);
        
        return operatore.getId();
    }
    
    // Hash della password con SecurityHelpers (PBKDF2)
    private String hashPassword(String password) {
        try {
            return SecurityHelpers.getPasswordHashPBKDF2(password);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore durante hash password con PBKDF2", ex);
            try {
                return SecurityHelpers.getPasswordHashSHA(password);
            } catch (Exception ex2) {
                logger.log(Level.SEVERE, "Errore durante hash password con SHA", ex2);
                return null;
            }
        }
    }
    
    // Verifica se l'utente corrente è un amministratore
    private boolean isUserAdministrator(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        
        String userRole = (String) session.getAttribute("user_role");
        if ("admin".equals(userRole) || "amministratore".equals(userRole)) {
            return true;
        }
        
        String userType = (String) session.getAttribute("user_type");
        if ("admin".equals(userType) || "amministratore".equals(userType)) {
            return true;
        }
        
        Object user = session.getAttribute("user");
        if (user != null) {
            String className = user.getClass().getSimpleName();
            if ("AmministratoreImpl".equals(className) || "Amministratore".equals(className)) {
                return true;
            }
        }
        
        return false;
    }
    
    // Ottiene l'amministratore corrente dalla sessione
    private Amministratore getCurrentAdministrator(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        
        Object user = session.getAttribute("user");
        if (user instanceof Amministratore) {
            return (Amministratore) user;
        }
        
        Integer userId = (Integer) session.getAttribute("userid");
        String fullName = (String) session.getAttribute("full_name");
        
        if (userId != null) {
            AmministratoreImpl tempAdmin = new AmministratoreImpl();
            tempAdmin.setId(userId);
            if (fullName != null) {
                String[] nameParts = fullName.split(" ", 2);
                tempAdmin.setNome(nameParts[0]);
                if (nameParts.length > 1) {
                    tempAdmin.setCognome(nameParts[1]);
                } else {
                    tempAdmin.setCognome("");
                }
            }
            return tempAdmin;
        }
        
        return null;
    }
    
    // Utility per redirect con errore
    private void redirectWithError(HttpServletResponse response, String errorCode, HttpServletRequest request) 
            throws Exception {
        String contextPath = request.getContextPath();
        String errorUrl = contextPath + "/admin/registrazione?error=" + errorCode;
        response.sendRedirect(errorUrl);
    }
    
    // Messaggi di errore localizzati
    private String getErrorMessage(String errorCode) {
        switch (errorCode) {
            case "access_denied":
                return "Accesso negato. Solo gli amministratori possono registrare nuovi utenti.";
            case "session_error":
                return "Errore di sessione. Effettua nuovamente il login.";
            case "tipo_utente_required":
                return "Seleziona il tipo di utente da creare.";
            case "nome_invalid":
                return "Il nome deve essere lungo almeno 2 caratteri.";
            case "cognome_invalid":
                return "Il cognome deve essere lungo almeno 2 caratteri.";
            case "cf_invalid":
                return "Il codice fiscale non è valido.";
            case "email_invalid":
                return "L'indirizzo email non è valido.";
            case "password_too_short":
                return "La password deve essere lunga almeno 8 caratteri.";
            case "password_mismatch":
                return "Le password non coincidono.";
            case "email_already_exists":
                return "Esiste già un utente con questa email.";
            case "cf_already_exists":
                return "Esiste già un utente con questo codice fiscale.";
            case "password_hash_error":
                return "Errore durante la crittografia della password.";
            case "database_error":
                return "Errore nel salvataggio dei dati. Riprova più tardi.";
            case "system_error":
                return "Si è verificato un errore di sistema. Riprova più tardi.";
            default:
                return "Si è verificato un errore durante la registrazione.";
        }
    }
    
    // Messaggi di successo localizzati
    private String getSuccessMessage(String successCode) {
        switch (successCode) {
            case "amministratore_created":
                return "Nuovo amministratore creato con successo!";
            case "operatore_created":
                return "Nuovo operatore creato con successo con patenti e abilità associate!";
            default:
                return "Operazione completata con successo!";
        }
    }
    
    @Override
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        String method = request.getMethod();
        
        if ("POST".equals(method)) {
            try {
                processUserRegistration(request, response);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Errore durante processUserRegistration", ex);
                showRegistrationForm(request, response);
            }
        } else {
            showRegistrationForm(request, response);
        }
    }
}