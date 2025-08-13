package webengineering.nuovissimosoccorsoweb.service;

import webengineering.nuovissimosoccorsoweb.SoccorsoDataLayer;
import webengineering.nuovissimosoccorsoweb.model.*;
import webengineering.framework.data.DataException;
import webengineering.framework.security.SecurityHelpers;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Servizio di autenticazione condiviso tra MVC e REST. Centralizza tutta la
 * logica di autenticazione per evitare duplicazione.
 */
public class AuthenticationService {

    private static final Logger logger = Logger.getLogger(AuthenticationService.class.getName());

    /**
     * Classe per le informazioni utente autenticato.
     */
    public static class UserInfo {

        private final int id;
        private final String email;
        private final String userType;
        private final String role;
        private final String fullName;

        public UserInfo(int id, String email, String userType, String role, String fullName) {
            this.id = id;
            this.email = email;
            this.userType = userType;
            this.role = role;
            this.fullName = fullName;
        }

        // Getters
        public int getId() {
            return id;
        }

        public String getEmail() {
            return email;
        }

        public String getUserType() {
            return userType;
        }

        public String getRole() {
            return role;
        }

        public String getFullName() {
            return fullName;
        }
    }

    /**
     * Autentica un utente (amministratore o operatore). UNICA implementazione
     * condivisa tra MVC e REST.
     */
    public static UserInfo authenticateUser(String email, String password, SoccorsoDataLayer dataLayer) {
        try {
            String cleanEmail = SecurityHelpers.stripSlashes(email);

            // 1. Prova autenticazione amministratore
            UserInfo adminInfo = authenticateAdmin(cleanEmail, password, dataLayer);
            if (adminInfo != null) {
                return adminInfo;
            }

            // 2. Prova autenticazione operatore
            UserInfo operatorInfo = authenticateOperator(cleanEmail, password, dataLayer);
            if (operatorInfo != null) {
                return operatorInfo;
            }

            // 3. Fallback demo (rimuovi in produzione)
            return authenticateDemo(cleanEmail, password);

        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore durante autenticazione: " + email, ex);
            return null;
        }
    }

    /**
     * Autentica amministratore.
     */
    private static UserInfo authenticateAdmin(String email, String password, SoccorsoDataLayer dataLayer) {
        try {
            Amministratore admin = dataLayer.getAmministratoreDAO().getAmministratoreByEmail(email);
            if (admin != null) {
                // Prova PBKDF2, poi SHA come fallback
                boolean passwordValid = false;
                try {
                    passwordValid = SecurityHelpers.checkPasswordHashPBKDF2(password, admin.getPassword());
                } catch (Exception e) {
                    try {
                        passwordValid = SecurityHelpers.checkPasswordHashSHA(password, admin.getPassword());
                    } catch (Exception e2) {
                        logger.log(Level.WARNING, "Errore verifica password admin: " + e2.getMessage());
                    }
                }

                if (passwordValid) {
                    String fullName = admin.getNome() + " " + admin.getCognome();
                    return new UserInfo(admin.getId(), admin.getEmail(), "amministratore", "admin", fullName);
                }
            }
        } catch (DataException ex) {
            logger.log(Level.WARNING, "Errore autenticazione admin: " + ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * Autentica operatore.
     */
    private static UserInfo authenticateOperator(String email, String password, SoccorsoDataLayer dataLayer) {
        try {
            Operatore operatore = dataLayer.getOperatoreDAO().getOperatoreByEmail(email);
            if (operatore != null) {
                // Prova PBKDF2, poi SHA come fallback
                boolean passwordValid = false;
                try {
                    passwordValid = SecurityHelpers.checkPasswordHashPBKDF2(password, operatore.getPassword());
                } catch (Exception e) {
                    try {
                        passwordValid = SecurityHelpers.checkPasswordHashSHA(password, operatore.getPassword());
                    } catch (Exception e2) {
                        logger.log(Level.WARNING, "Errore verifica password operatore: " + e2.getMessage());
                    }
                }

                if (passwordValid) {
                    String fullName = operatore.getNome() + " " + operatore.getCognome();
                    return new UserInfo(operatore.getId(), operatore.getEmail(), "operatore", "operator", fullName);
                }
            }
        } catch (DataException ex) {
            logger.log(Level.WARNING, "Errore autenticazione operatore: " + ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * Autenticazione demo
     *
     */
    private static UserInfo authenticateDemo(String email, String password) {
        if ("admin@soccorso.it".equals(email) && "admin123".equals(password)) {
            return new UserInfo(1, "admin@soccorso.it", "amministratore", "admin", "Admin Demo");
        }
        if ("operatore@soccorso.it".equals(email) && "op123".equals(password)) {
            return new UserInfo(2, "operatore@soccorso.it", "operatore", "operator", "Operatore Demo");
        }
        return null;
    }

}
