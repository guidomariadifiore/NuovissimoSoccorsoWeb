package webengineering.nuovissimosoccorsoweb.rest.service;

import webengineering.nuovissimosoccorsoweb.SoccorsoDataLayer;
import webengineering.nuovissimosoccorsoweb.service.AuthenticationService;
import webengineering.framework.data.DataException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * AuthService REST - ora usa il servizio condiviso invece di duplicare codice.
 */
public class AuthService {
    
    private static final Logger logger = Logger.getLogger(AuthService.class.getName());
    private static AuthService instance;
    
    // Adapter class per compatibilità con il codice REST esistente
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
        
        // Costruttore da AuthenticationService.UserInfo
        public UserInfo(AuthenticationService.UserInfo authInfo) {
            this.id = authInfo.getId();
            this.email = authInfo.getEmail();
            this.role = authInfo.getRole();
            this.fullName = authInfo.getFullName();
        }
        
        public int getId() { return id; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public String getFullName() { return fullName; }
    }
    
    private AuthService() {}
    
    public static AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }
    
    /**
     * Autentica un utente - codice condiviso con MVC
     */
    public UserInfo authenticateUser(String email, String password) {
        SoccorsoDataLayer dataLayer = null;
        
        try {
            dataLayer = createDataLayer();
            
            AuthenticationService.UserInfo authResult = 
                AuthenticationService.authenticateUser(email, password, dataLayer);
            
            if (authResult != null) {
                // Converte al formato REST UserInfo
                return new UserInfo(authResult);
            }
            
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
     * Crea DataLayer (metodo helper).
     */
    private SoccorsoDataLayer createDataLayer() throws NamingException, SQLException, DataException {
        InitialContext ctx = new InitialContext();
        DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/soccorso");
        SoccorsoDataLayer dataLayer = new SoccorsoDataLayer(ds);
        dataLayer.init();
        return dataLayer;
    }
}