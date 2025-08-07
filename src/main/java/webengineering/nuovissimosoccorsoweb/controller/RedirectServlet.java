package webengineering.nuovissimosoccorsoweb.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.logging.Logger;

public class RedirectServlet extends HttpServlet {
    
    private static final Logger logger = Logger.getLogger(RedirectServlet.class.getName());
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String contextPath = request.getContextPath();
        
        // Preserva eventuali parametri di query string
        String queryString = request.getQueryString();
        String redirectUrl = contextPath + "/emergenza";
        
        if (queryString != null && !queryString.isEmpty()) {
            redirectUrl += "?" + queryString;
        }
        
        logger.info("Redirect da " + request.getRequestURI() + " verso " + redirectUrl);
        response.sendRedirect(redirectUrl);
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        // Anche i POST vengono reindirizzati
        doGet(request, response);
    }
}