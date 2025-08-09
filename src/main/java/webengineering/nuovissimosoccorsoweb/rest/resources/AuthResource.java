package webengineering.nuovissimosoccorsoweb.rest.resources;

import jakarta.servlet.ServletContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import webengineering.nuovissimosoccorsoweb.rest.security.JWTHelper;
import webengineering.nuovissimosoccorsoweb.rest.security.Secured;
import webengineering.nuovissimosoccorsoweb.rest.service.AuthService;

/**
 * Resource REST per la gestione dell'autenticazione.
 * Endpoint: /api/auth/*
 * 
 * Solo per amministratori e operatori.
 * 
 * @author YourName
 */
@Path("auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {
    
    @Context
    private ServletContext context;
    
    /**
     * Endpoint per il login.
     * POST /api/auth/login
     * 
     * @param loginRequest Credenziali di login
     * @return Token JWT se login successful
     */
    @POST
    @Path("login")
    public Response login(LoginRequest loginRequest) {
        try {
            // DEBUG: Log della richiesta ricevuta
            System.out.println("=== DEBUG LOGIN ===");
            System.out.println("LoginRequest ricevuto: " + loginRequest);
            if (loginRequest != null) {
                System.out.println("Email: " + loginRequest.getEmail());
                System.out.println("Password: " + (loginRequest.getPassword() != null ? "[PRESENTE]" : "[NULL]"));
            }
            
            // Validazione input
            if (loginRequest == null || 
                loginRequest.getEmail() == null || loginRequest.getEmail().trim().isEmpty() ||
                loginRequest.getPassword() == null || loginRequest.getPassword().trim().isEmpty()) {
                
                System.out.println("Validazione fallita: dati mancanti");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new LoginResponse(false, "Email e password sono richiesti", null, null))
                        .build();
            }
            
            // Usa AuthService che integra con il database reale
            AuthService.UserInfo authResult = AuthService.getInstance()
                .authenticateUser(loginRequest.getEmail(), loginRequest.getPassword());
            
            if (authResult == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new LoginResponse(false, "Credenziali non valide", null, null))
                        .build();
            }
            
            // Converti il risultato in UserInfo per la risposta
            UserInfo user = new UserInfo(authResult.getId(), authResult.getEmail(), authResult.getRole());
            
            // Genera token JWT (converte int to Long per compatibilità)
            String token = JWTHelper.generateToken((long) user.getId(), user.getEmail(), user.getRole());
            
            // Risposta di successo
            LoginResponse response = new LoginResponse(true, "Login effettuato con successo", token, user);
            
            return Response.ok(response)
                    .header("Authorization", "Bearer " + token)
                    .build();
                    
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new LoginResponse(false, "Errore interno del server", null, null))
                    .build();
        }
    }
    
    /**
     * Endpoint per il logout.
     * DELETE /api/auth/logout
     */
    @DELETE
    @Path("logout")
    @Secured
    public Response logout() {
        // Con JWT stateless, il logout è gestito lato client
        // Il token viene semplicemente rimosso dal client
        return Response.ok()
                .entity(new LogoutResponse(true, "Logout effettuato con successo"))
                .build();
    }
    
    /**
     * Endpoint per verificare la validità del token.
     * GET /api/auth/verify
     */
    @GET
    @Path("verify")
    @Secured
    public Response verifyToken(@Context jakarta.ws.rs.container.ContainerRequestContext requestContext) {
        // Se arriviamo qui, il token è valido (filtro JWT ha già verificato)
        String username = (String) requestContext.getProperty("username");
        Integer userId = (Integer) requestContext.getProperty("userId");
        String role = (String) requestContext.getProperty("userRole");
        
        UserInfo user = new UserInfo(userId, username, role);
        
        return Response.ok()
                .entity(new VerifyResponse(true, "Token valido", user))
                .build();
    }
    
    /**
     * Endpoint di test per verificare che JSON funzioni.
     * GET /api/auth/test
     */
    @GET
    @Path("test")
    public Response test() {
        return Response.ok()
                .entity("{\"message\":\"REST API funziona!\",\"timestamp\":" + System.currentTimeMillis() + "}")
                .build();
    }
    
    // ========== CLASSI DTO (Data Transfer Objects) ==========
    
    /**
     * Classe per la richiesta di login.
     */
    public static class LoginRequest {
        private String email;
        private String password;
        
        // Costruttori
        public LoginRequest() {}
        
        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
        
        // Getter e Setter per Jackson
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
    
    /**
     * Classe per la risposta di login.
     */
    public static class LoginResponse {
        private boolean success;
        private String message;
        private String token;
        private UserInfo user;
        
        public LoginResponse(boolean success, String message, String token, UserInfo user) {
            this.success = success;
            this.message = message;
            this.token = token;
            this.user = user;
        }
        
        // Getter per Jackson
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getToken() { return token; }
        public UserInfo getUser() { return user; }
    }
    
    /**
     * Classe per la risposta di logout.
     */
    public static class LogoutResponse {
        private boolean success;
        private String message;
        
        public LogoutResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        // Getter per Jackson
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
    
    /**
     * Classe per la risposta di verifica token.
     */
    public static class VerifyResponse {
        private boolean valid;
        private String message;
        private UserInfo user;
        
        public VerifyResponse(boolean valid, String message, UserInfo user) {
            this.valid = valid;
            this.message = message;
            this.user = user;
        }
        
        // Getter per Jackson
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public UserInfo getUser() { return user; }
    }
    
    /**
     * Classe per rappresentare le informazioni dell'utente.
     * Solo amministratori e operatori.
     */
    public static class UserInfo {
        private final int id;
        private final String email;
        private final String role; // "ADMIN" o "OPERATORE"
        
        public UserInfo(int id, String email, String role) {
            this.id = id;
            this.email = email;
            this.role = role;
        }
        
        // Getter per Jackson
        public int getId() { return id; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
    }
}