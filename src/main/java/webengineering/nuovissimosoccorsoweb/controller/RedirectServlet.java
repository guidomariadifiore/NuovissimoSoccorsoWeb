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
        
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        
        // Rimuovi context path per ottenere il path relativo
        String relativePath = requestURI.substring(contextPath.length());
        
        // ========== ESCLUSIONI: NON reindirizzare questi file ==========
        
        // 1. File statici (CSS, JS, immagini, etc.)
        if (isStaticFile(relativePath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File non trovato: " + relativePath);
            return;
        }
        
        // 2. Client di test REST
        if (relativePath.equals("/rest-client-test.html")) {
            // Serve il file direttamente
            request.getRequestDispatcher("/rest-client-test.html").forward(request, response);
            return;
        }
        
        // 3. API REST (già gestite da Jersey, ma per sicurezza)
        if (relativePath.startsWith("/api/")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "API endpoint non trovato: " + relativePath);
            return;
        }
        
        // ========== REDIRECT DEFAULT ==========
        
        String queryString = request.getQueryString();
        String redirectUrl = contextPath + "/emergenza";
        
        if (queryString != null && !queryString.isEmpty()) {
            redirectUrl += "?" + queryString;
        }
        
        logger.info("Redirect da " + requestURI + " verso " + redirectUrl);
        response.sendRedirect(redirectUrl);
    }
    
    /**
     * Verifica se il path è per un file statico.
     */
    private boolean isStaticFile(String path) {
        if (path == null || path.length() <= 1) {
            return false;
        }
        
        // File con estensioni statiche comuni
        String[] staticExtensions = {
            ".css", ".js", ".html", ".htm", 
            ".jpg", ".jpeg", ".png", ".gif", ".ico", ".svg",
            ".woff", ".woff2", ".ttf", ".otf",
            ".pdf", ".txt", ".xml"
        };
        
        String lowerPath = path.toLowerCase();
        for (String ext : staticExtensions) {
            if (lowerPath.endsWith(ext)) {
                return true;
            }
        }
        
        // Directory statiche comuni
        if (lowerPath.startsWith("/css/") || 
            lowerPath.startsWith("/js/") || 
            lowerPath.startsWith("/images/") || 
            lowerPath.startsWith("/img/") ||
            lowerPath.startsWith("/assets/") ||
            lowerPath.startsWith("/static/")) {
            return true;
        }
        
        return false;
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        // I POST non dovrebbero essere reindirizzati, ma gestiamo comunque
        doGet(request, response);
    }
}