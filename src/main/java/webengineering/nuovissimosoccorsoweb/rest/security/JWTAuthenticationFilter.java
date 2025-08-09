package webengineering.nuovissimosoccorsoweb.rest.security;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * Filtro JWT per l'autenticazione delle API REST.
 * Si attiva automaticamente per tutti i metodi annotati con @Secured.
 */
@Provider
@Secured
@Priority(Priorities.AUTHENTICATION)
public class JWTAuthenticationFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String token = null;
        
        // Cerca il token nell'header Authorization
        String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            token = authorizationHeader.substring("Bearer ".length()).trim();
        }
        
        if (token == null || token.isEmpty()) {
            // Token mancante
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"Token di autenticazione mancante\"}")
                    .build()
            );
            return;
        }
        
        try {
            // Valida il token usando JWTHelper
            if (!JWTHelper.isTokenValid(token)) {
                requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\": \"Token non valido o scaduto\"}")
                        .build()
                );
                return;
            }
            
            // Estrai informazioni dal token e inseriscile nel context
            String email = JWTHelper.getEmailFromToken(token);
            Long userId = JWTHelper.getUserIdFromToken(token);
            String role = JWTHelper.getRoleFromToken(token);
            
            // Popola le properties che verranno usate dagli endpoint
            requestContext.setProperty("token", token);
            requestContext.setProperty("username", email);
            requestContext.setProperty("userId", userId != null ? userId.intValue() : null);
            requestContext.setProperty("userRole", role);
            
        } catch (Exception e) {
            // Errore nella validazione del token
            System.err.println("Errore validazione JWT: " + e.getMessage());
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"Errore nella validazione del token\"}")
                    .build()
            );
        }
    }
}