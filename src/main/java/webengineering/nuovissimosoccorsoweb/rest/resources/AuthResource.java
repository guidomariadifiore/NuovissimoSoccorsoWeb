package webengineering.nuovissimosoccorsoweb.rest.resources;

import jakarta.servlet.ServletContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import webengineering.nuovissimosoccorsoweb.rest.security.JWTHelper;
import webengineering.nuovissimosoccorsoweb.rest.security.Secured;

/**
 * Resource REST per la gestione dell'autenticazione.
 * Endpoint: /api/auth/*
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
            // Validazione input
            if (loginRequest == null || 
                loginRequest.getEmail() == null || loginRequest.getEmail().trim().isEmpty() ||
                loginRequest.getPassword() == null || loginRequest.getPassword().trim().isEmpty()) {
                
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new LoginResponse(false, "Email e password sono richiesti", null, null))
                        .build();
            }
            
            // TODO: Qui dovrai integrare con il tuo sistema di autenticazione esistente
            // Per ora simuliamo la validazione
            UserInfo user = validateUser(loginRequest.getEmail(), loginRequest.getPassword());
            
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new LoginResponse(false, "Credenziali non valide", null, null))
                        .build();
            }
            
            // Genera token JWT
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
     * Metodo per validare le credenziali utente.
     * TODO: Integrare con il tuo sistema di autenticazione esistente.
     */
    private UserInfo validateUser(String email, String password) {
        // PLACEHOLDER - Sostituire con la logica reale di validazione
        // Dovrai integrare con il tuo data layer esistente
        
        if ("admin@soccorso.it".equals(email) && "admin123".equals(password)) {
            return new UserInfo(1, email, "ADMIN");
        } else if ("operatore@soccorso.it".equals(email) && "op123".equals(password)) {
            return new UserInfo(2, email, "OPERATORE");
        } else if ("utente@soccorso.it".equals(email) && "user123".equals(password)) {
            return new UserInfo(3, email, "UTENTE");
        }
        
        return null; // Credenziali non valide
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
     */
    public static class UserInfo {
        private int id;
        private String email;
        private String role;
        
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