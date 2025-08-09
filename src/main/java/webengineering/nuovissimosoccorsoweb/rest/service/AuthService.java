package webengineering.nuovissimosoccorsoweb.rest.service;

import webengineering.nuovissimosoccorsoweb.SoccorsoDataLayer;
import webengineering.nuovissimosoccorsoweb.model.*;
import webengineering.framework.data.DataException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.logging.Level;
import webengineering.nuovissimosoccorsoweb.dao.AmministratoreDAO;
import webengineering.nuovissimosoccorsoweb.dao.OperatoreDAO;

/**
 * Servizio di autenticazione che integra con il sistema MVC esistente.
 * Usa lo stesso DataLayer e logica di autenticazione del progetto principale.
 * 
 * @author YourName
 */
public class AuthService {
    
    private static final Logger logger = Logger.getLogger(AuthService.class.getName());
    private static AuthService instance;
    
    private AuthService() {}
    
    public static AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }
    
    /**
     * Autentica un utente usando lo stesso sistema del progetto MVC.
     * 
     * @param email Email dell'utente
     * @param password Password in chiaro
     * @return UserInfo se autenticazione riuscita, null altrimenti
     */
    public UserInfo authenticateUser(String email, String password) {
        SoccorsoDataLayer dataLayer = null;
        
        try {
            // Ottieni DataSource (stesso modo del progetto MVC)
            dataLayer = createDataLayer();
            
            // Autenticazione amministratori
            UserInfo adminInfo = authenticateAdmin(email, password, dataLayer);
            if (adminInfo != null) {
                return adminInfo;
            }
            
            // Autenticazione operatori
            UserInfo operatorInfo = authenticateOperator(email, password, dataLayer);
            if (operatorInfo != null) {
                return operatorInfo;
            }
            
            // Nessuna autenticazione riuscita
            return null;
            
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore durante autenticazione REST: " + ex.getMessage(), ex);
            return null;
        } finally {
            if (dataLayer != null) {
                try {
                    dataLayer.destroy();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Errore chiusura DataLayer", e);
                }
            }
        }
    }
    
    /**
     * Crea DataLayer come nel progetto MVC.
     */
    private SoccorsoDataLayer createDataLayer() throws NamingException, SQLException {
        InitialContext ctx = new InitialContext();
        DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/soccorso");
        return new SoccorsoDataLayer(ds);
    }
    
    /**
     * Autentica come amministratore (stesso codice del Login.java).
     */
    private UserInfo authenticateAdmin(String email, String password, SoccorsoDataLayer dataLayer) {
        try {
            AmministratoreDAO adminDAO = dataLayer.getAmministratoreDAO();
            Amministratore admin = adminDAO.getAmministratoreByEmail(email);
            
            if (admin != null) {
                // Prova prima PBKDF2, poi SHA (come nel progetto originale)
                boolean passwordValid = false;
                try {
                    passwordValid = webengineering.framework.security.SecurityHelpers.checkPasswordHashPBKDF2(password, admin.getPassword());
                } catch (Exception e) {
                    // Se PBKDF2 fallisce, prova SHA
                    try {
                        passwordValid = webengineering.framework.security.SecurityHelpers.checkPasswordHashSHA(password, admin.getPassword());
                    } catch (Exception e2) {
                        logger.log(Level.WARNING, "Errore verifica password admin: " + e2.getMessage());
                    }
                }
                
                if (passwordValid) {
                    return new UserInfo(
                        admin.getId(),  // ← Corretto: getId() invece di getKey()
                        admin.getEmail(),
                        "ADMIN",
                        admin.getNome() + " " + admin.getCognome()
                    );
                }
            }
        } catch (DataException ex) {
            logger.log(Level.WARNING, "Errore autenticazione admin: " + ex.getMessage(), ex);
        }
        return null;
    }
    
    /**
     * Autentica come operatore (stesso codice del Login.java).
     */
    private UserInfo authenticateOperator(String email, String password, SoccorsoDataLayer dataLayer) {
        try {
            OperatoreDAO operatoreDAO = dataLayer.getOperatoreDAO();
            Operatore operatore = operatoreDAO.getOperatoreByEmail(email);
            
            if (operatore != null) {
                // Prova prima PBKDF2, poi SHA (come nel progetto originale)
                boolean passwordValid = false;
                try {
                    passwordValid = webengineering.framework.security.SecurityHelpers.checkPasswordHashPBKDF2(password, operatore.getPassword());
                } catch (Exception e) {
                    // Se PBKDF2 fallisce, prova SHA
                    try {
                        passwordValid = webengineering.framework.security.SecurityHelpers.checkPasswordHashSHA(password, operatore.getPassword());
                    } catch (Exception e2) {
                        logger.log(Level.WARNING, "Errore verifica password operatore: " + e2.getMessage());
                    }
                }
                
                if (passwordValid) {
                    return new UserInfo(
                        operatore.getId(),  // ← Corretto: getId() invece di getKey()
                        operatore.getEmail(),
                        "OPERATORE",
                        operatore.getNome() + " " + operatore.getCognome()
                    );
                }
            }
        } catch (DataException ex) {
            logger.log(Level.WARNING, "Errore autenticazione operatore: " + ex.getMessage(), ex);
        }
        return null;
    }
    
    /**
     * Classe per le informazioni utente (compatibile con AuthResource).
     */
    public static class UserInfo {
        private final int id;
        private final String email;
        private final String role;
        private final String fullName;
        
        public UserInfo(int id, String email, String role, String fullName) {
            this.id = id;
            this.email = email;
            this.role = role;
            this.fullName = fullName;
        }
        
        // Getter
        public int getId() { return id; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public String getFullName() { return fullName; }
    }
}