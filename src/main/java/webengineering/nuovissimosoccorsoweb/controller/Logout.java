package webengineering.nuovissimosoccorsoweb.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.logging.Logger;
import webengineering.framework.security.SecurityHelpers;

public class Logout extends SoccorsoBaseController {
    
    private static final Logger logger = Logger.getLogger(Logout.class.getName());
    
    private void action_logout(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        // Log dell'utente che sta facendo logout (opzionale)
        HttpSession session = request.getSession(false);
        if (session != null) {
            String username = (String) session.getAttribute("username");
            logger.info("Logout utente: " + username);
        }
        
        // Invalida la sessione usando il framework
        SecurityHelpers.disposeSession(request);
        
        // Redirect con messaggio di successo
        String contextPath = request.getContextPath();
        response.sendRedirect(contextPath + "/login?logout=success");
    }
    
    @Override
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        try {
            action_logout(request, response);
        } catch (IOException ex) {
            handleError(ex, request, response);
        }
    }
}