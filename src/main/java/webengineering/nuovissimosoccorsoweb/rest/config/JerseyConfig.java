package webengineering.nuovissimosoccorsoweb.rest.config;

import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Configurazione semplificata di Jersey.
 * Auto-discovery: Jersey trova automaticamente tutte le risorse nei package specificati.
 * 
 * Trova automaticamente:
 * - @Path: AuthResource, RichiesteResource, etc. 
 * - @Provider: JWTAuthenticationFilter, CORSFilter, AppExceptionMapper, JacksonExceptionMapper
 * 
 */
@ApplicationPath("api")
public class JerseyConfig extends ResourceConfig {
    
    public JerseyConfig() {
        // Auto-discovery: Jersey scannerizza questi package e registra automaticamente:
        // - Tutte le classi con @Path (resources)  
        // - Tutte le classi con @Provider (filtri, exception mapper, ecc.)
        packages("webengineering.nuovissimosoccorsoweb.rest");
        
        // Registra esplicitamente Jackson per JSON
        register(com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider.class);
        
        // Registra il modulo per Java 8 Time (LocalDateTime, etc.)
        register(com.fasterxml.jackson.datatype.jsr310.JavaTimeModule.class);
    }
}