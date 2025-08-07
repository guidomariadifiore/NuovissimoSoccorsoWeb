package webengineering.nuovissimosoccorsoweb.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import webengineering.framework.data.DataLayer;
import webengineering.nuovissimosoccorsoweb.SoccorsoDataLayer;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Logger;
import webengineering.framework.controller.AbstractBaseController;

public abstract class SoccorsoBaseController extends AbstractBaseController {

    @Override
    protected DataLayer createDataLayer(DataSource ds) throws ServletException {
        try {
            return new SoccorsoDataLayer(ds);
        } catch (SQLException ex) {
            throw new ServletException("Errore nella creazione del SoccorsoDataLayer", ex);
        }
    }
    
    /**
     * Aggiunge le informazioni utente al data model per i template. Lo scrivo qui al posto di ripeterlo per ogni controller
     */
    protected void addUserInfoToModel(HttpServletRequest request, Map<String, Object> dataModel) {
        HttpSession session = request.getSession(false);

        // DEBUG: Log delle informazioni sessione
        Logger logger = Logger.getLogger(this.getClass().getName());
        logger.info("=== DEBUG addUserInfoToModel ===");
        logger.info("Session: " + (session != null ? session.getId() : "null"));

        // Verifica se l'utente è loggato
        boolean isLoggedIn = (session != null && session.getAttribute("userid") != null);
        logger.info("isLoggedIn: " + isLoggedIn);

        dataModel.put("user_logged_in", isLoggedIn);

        if (isLoggedIn) {
            // Informazioni utente dalla sessione
            String username = (String) session.getAttribute("username");
            String fullName = (String) session.getAttribute("full_name");
            String userRole = (String) session.getAttribute("user_role");
            String userType = (String) session.getAttribute("user_type");

            logger.info("username: " + username);
            logger.info("fullName: " + fullName);
            logger.info("userRole: " + userRole);
            logger.info("userType: " + userType);

            // Aggiungi al model
            dataModel.put("username", username);
            dataModel.put("user_display_name", fullName != null ? fullName : username);
            dataModel.put("user_role", userRole);
            dataModel.put("user_type", userType);

            // Determina il tipo di utente (usa la stessa logica del filtro)
            boolean isAdmin = "admin".equals(userRole) || "amministratore".equals(userType);
            boolean isOperator = "operator".equals(userRole) || "operatore".equals(userType);

            logger.info("isAdmin: " + isAdmin);
            logger.info("isOperator: " + isOperator);

            dataModel.put("is_admin", isAdmin);
            dataModel.put("is_operator", isOperator);

            // Display del ruolo
            String roleDisplay = isAdmin ? "Amministratore" : (isOperator ? "Operatore" : "Utente");
            dataModel.put("user_role_display", roleDisplay);

            logger.info("roleDisplay: " + roleDisplay);
        }

        // URL per login/logout
        String contextPath = request.getContextPath();
        logger.info("contextPath: '" + contextPath + "'");

        String loginUrl = contextPath + "/login";
        String logoutUrl = contextPath + "/logout";

        logger.info("loginUrl: " + loginUrl);
        logger.info("logoutUrl: " + logoutUrl);

        dataModel.put("login_url", loginUrl);
        dataModel.put("logout_url", logoutUrl);

        logger.info("=== END DEBUG addUserInfoToModel ===");
    }
}
