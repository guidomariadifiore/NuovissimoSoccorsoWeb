package webengineering.nuovissimosoccorsoweb.util;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Filtro per l'autenticazione e l'autorizzazione degli utenti
 * - /admin/* riservato agli amministratori
 * - /operatore/* riservato agli operatori
 */
public class AuthenticationFilter implements Filter {
    
    private static final Logger logger = Logger.getLogger(AuthenticationFilter.class.getName());
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("AuthenticationFilter inizializzato");
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestURI = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        
        // Rimuovi il context path per ottenere il path relativo
        String relativePath = requestURI.substring(contextPath.length());
        
        logger.info("AuthenticationFilter: Processing " + relativePath);
        
        // Verifica se la richiesta è per un'area protetta
        if (relativePath.startsWith("/admin/")) {
            if (!isUserAdmin(httpRequest)) {
                logger.warning("Accesso negato ad area admin per: " + relativePath);
                redirectToLogin(httpResponse, contextPath, "access_denied");
                return;
            }
            logger.info("Accesso admin autorizzato per: " + relativePath);
        } 
        else if (relativePath.startsWith("/operatore/")) {
            if (!isUserOperator(httpRequest)) {
                logger.warning("Accesso negato ad area operatore per: " + relativePath);
                redirectToLogin(httpResponse, contextPath, "access_denied");
                return;
            }
            logger.info("Accesso operatore autorizzato per: " + relativePath);
        }
        
        // Se arriviamo qui, l'utente è autorizzato o la richiesta non è per un'area protetta
        chain.doFilter(request, response);
    }
    
    /**
     * Verifica se l'utente corrente è un amministratore
     */
    private boolean isUserAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            logger.info("Nessuna sessione trovata");
            return false;
        }
        
        // Metodo 1: Verifica tramite user_role
        String userRole = (String) session.getAttribute("user_role");
        boolean isAdminByRole = "admin".equals(userRole) || "amministratore".equals(userRole);
        
        // Metodo 2: Verifica tramite user_type
        String userType = (String) session.getAttribute("user_type");
        boolean isAdminByType = "admin".equals(userType) || "amministratore".equals(userType);
        
        // Metodo 3: Verifica tramite oggetto utente (se presente)
        Object user = session.getAttribute("user");
        boolean isAdminByObject = false;
        if (user != null) {
            String className = user.getClass().getSimpleName();
            isAdminByObject = "AmministratoreImpl".equals(className) || "Amministratore".equals(className);
        }
        
        boolean isAdmin = isAdminByRole || isAdminByType || isAdminByObject;
        
        logger.info("Verifica admin: user=" + (user != null ? user.getClass().getSimpleName() : "null") + 
                   ", role=" + userRole + ", type=" + userType + 
                   ", isAdminByRole=" + isAdminByRole +
                   ", isAdminByType=" + isAdminByType +
                   ", isAdminByObject=" + isAdminByObject +
                   ", RISULTATO=" + isAdmin);
        
        return isAdmin;
    }
    
    /**
     * Verifica se l'utente corrente è un operatore
     */
    private boolean isUserOperator(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            logger.info("Nessuna sessione trovata");
            return false;
        }
        
        // Metodo 1: Verifica tramite user_role
        String userRole = (String) session.getAttribute("user_role");
        boolean isOperatorByRole = "operator".equals(userRole) || "operatore".equals(userRole);
        
        // Metodo 2: Verifica tramite user_type
        String userType = (String) session.getAttribute("user_type");
        boolean isOperatorByType = "operator".equals(userType) || "operatore".equals(userType);
        
        // Metodo 3: Verifica tramite oggetto utente (se presente)
        Object user = session.getAttribute("user");
        boolean isOperatorByObject = false;
        if (user != null) {
            String className = user.getClass().getSimpleName();
            isOperatorByObject = "OperatoreImpl".equals(className) || "Operatore".equals(className);
        }
        
        boolean isOperator = isOperatorByRole || isOperatorByType || isOperatorByObject;
        
        logger.info("Verifica operatore: user=" + (user != null ? user.getClass().getSimpleName() : "null") + 
                   ", role=" + userRole + ", type=" + userType + 
                   ", isOperatorByRole=" + isOperatorByRole +
                   ", isOperatorByType=" + isOperatorByType +
                   ", isOperatorByObject=" + isOperatorByObject +
                   ", RISULTATO=" + isOperator);
        
        return isOperator;
    }
    
    /**
     * Reindirizza al login con messaggio di errore
     */
    private void redirectToLogin(HttpServletResponse response, String contextPath, String errorCode) 
            throws IOException {
        String loginUrl = contextPath + "/login?error=" + errorCode;
        logger.info("Reindirizzamento a: " + loginUrl);
        response.sendRedirect(loginUrl);
    }
    
    @Override
    public void destroy() {
        logger.info("AuthenticationFilter distrutto");
    }
}