package webengineering.nuovissimosoccorsoweb.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.logging.Logger;

/**
 * Servlet per il debug della sessione
 * Mostra tutti gli attributi presenti nella sessione corrente
 */
public class SessionDebug extends HttpServlet {
    
    private static final Logger logger = Logger.getLogger(SessionDebug.class.getName());
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        out.println("<!DOCTYPE html>");
        out.println("<html><head><title>Session Debug</title></head><body>");
        out.println("<h1>🔍 Session Debug Information</h1>");
        
        HttpSession session = request.getSession(false);
        
        if (session == null) {
            out.println("<p style='color: red;'><strong>❌ Nessuna sessione attiva!</strong></p>");
            out.println("<p><a href='" + request.getContextPath() + "/login'>Vai al Login</a></p>");
        } else {
            out.println("<p style='color: green;'><strong>✅ Sessione attiva</strong></p>");
            out.println("<p><strong>Session ID:</strong> " + session.getId() + "</p>");
            out.println("<p><strong>Created:</strong> " + new java.util.Date(session.getCreationTime()) + "</p>");
            out.println("<p><strong>Last Accessed:</strong> " + new java.util.Date(session.getLastAccessedTime()) + "</p>");
            out.println("<p><strong>Max Inactive Interval:</strong> " + session.getMaxInactiveInterval() + " secondi</p>");
            
            out.println("<h2>📋 Attributi in Sessione:</h2>");
            
            Enumeration<String> attributeNames = session.getAttributeNames();
            boolean hasAttributes = false;
            
            out.println("<table border='1' style='border-collapse: collapse; width: 100%;'>");
            out.println("<tr><th>Nome Attributo</th><th>Valore</th><th>Tipo</th></tr>");
            
            while (attributeNames.hasMoreElements()) {
                hasAttributes = true;
                String attrName = attributeNames.nextElement();
                Object attrValue = session.getAttribute(attrName);
                
                out.println("<tr>");
                out.println("<td><strong>" + attrName + "</strong></td>");
                out.println("<td>" + (attrValue != null ? attrValue.toString() : "null") + "</td>");
                out.println("<td>" + (attrValue != null ? attrValue.getClass().getSimpleName() : "null") + "</td>");
                out.println("</tr>");
                
                // Log anche nel server
                logger.info("Session attribute: " + attrName + " = " + attrValue + 
                           " (type: " + (attrValue != null ? attrValue.getClass().getName() : "null") + ")");
            }
            
            out.println("</table>");
            
            if (!hasAttributes) {
                out.println("<p style='color: orange;'><strong>⚠️ Nessun attributo trovato in sessione!</strong></p>");
            }
            
            // Test di autorizzazione
            out.println("<h2>🔐 Test Autorizzazioni:</h2>");
            
            Object user = session.getAttribute("user");
            String userRole = (String) session.getAttribute("user_role");
            String userType = (String) session.getAttribute("user_type");
            
            out.println("<p><strong>User object:</strong> " + (user != null ? user.getClass().getSimpleName() : "null") + "</p>");
            out.println("<p><strong>User role:</strong> " + userRole + "</p>");
            out.println("<p><strong>User type:</strong> " + userType + "</p>");
            
            // Test specifici - AGGIORNATI per usare la stessa logica del filtro
            // userRole e userType sono già dichiarate sopra
            
            // Test admin con la stessa logica del filtro
            boolean isAdminByRole = "admin".equals(userRole) || "amministratore".equals(userRole);
            boolean isAdminByType = "admin".equals(userType) || "amministratore".equals(userType);
            boolean isAdminByObject = false;
            if (user != null) {
                String className = user.getClass().getSimpleName();
                isAdminByObject = "AmministratoreImpl".equals(className) || "Amministratore".equals(className);
            }
            boolean isAdmin = isAdminByRole || isAdminByType || isAdminByObject;
            
            // Test operatore con la stessa logica del filtro
            boolean isOperatorByRole = "operator".equals(userRole) || "operatore".equals(userRole);
            boolean isOperatorByType = "operator".equals(userType) || "operatore".equals(userType);
            boolean isOperatorByObject = false;
            if (user != null) {
                String className = user.getClass().getSimpleName();
                isOperatorByObject = "OperatoreImpl".equals(className) || "Operatore".equals(className);
            }
            boolean isOperator = isOperatorByRole || isOperatorByType || isOperatorByObject;
            
            out.println("<p style='color: " + (isAdmin ? "green" : "red") + ";'><strong>È Admin:</strong> " + isAdmin + "</p>");
            out.println("<p style='color: " + (isOperator ? "green" : "red") + ";'><strong>È Operatore:</strong> " + isOperator + "</p>");
            
            // Dettagli per il debug
            out.println("<h3>📊 Dettagli Test Admin:</h3>");
            out.println("<p>• <strong>Admin by role:</strong> " + isAdminByRole + " (role='" + userRole + "')</p>");
            out.println("<p>• <strong>Admin by type:</strong> " + isAdminByType + " (type='" + userType + "')</p>");
            out.println("<p>• <strong>Admin by object:</strong> " + isAdminByObject + " (object=" + (user != null ? user.getClass().getSimpleName() : "null") + ")</p>");
            
            out.println("<h3>📊 Dettagli Test Operatore:</h3>");
            out.println("<p>• <strong>Operator by role:</strong> " + isOperatorByRole + " (role='" + userRole + "')</p>");
            out.println("<p>• <strong>Operator by type:</strong> " + isOperatorByType + " (type='" + userType + "')</p>");
            out.println("<p>• <strong>Operator by object:</strong> " + isOperatorByObject + " (object=" + (user != null ? user.getClass().getSimpleName() : "null") + ")</p>");
        }
        
        out.println("<h2>🔗 Link di Test:</h2>");
        out.println("<ul>");
        out.println("<li><a href='" + request.getContextPath() + "/login'>Login</a></li>");
        out.println("<li><a href='" + request.getContextPath() + "/admin/registrazione'>Admin - Registrazione</a></li>");
        out.println("<li><a href='" + request.getContextPath() + "/operatore/dashboard'>Operatore - Dashboard</a></li>");
        out.println("<li><a href='" + request.getContextPath() + "/emergenza'>Emergenza (pubblico)</a></li>");
        out.println("</ul>");
        
        out.println("<p><a href='javascript:location.reload();'>🔄 Ricarica</a></p>");
        
        out.println("</body></html>");
    }
}