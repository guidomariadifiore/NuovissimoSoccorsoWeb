package webengineering.nuovissimosoccorsoweb.rest.config;

import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Configurazione semplificata di Jersey.
 * Auto-discovery: Jersey trova automaticamente tutte le risorse nei package specificati.
 * 
 */
@ApplicationPath("api")
public class JerseyConfig extends ResourceConfig {
    
    public JerseyConfig() {
        // Auto-discovery: Jersey scannerizza questi package e registra automaticamente:
        // - @Path (resources)
        // - @Provider (filtri, exception mapper, ecc.)
        packages("webengineering.nuovissimosoccorsoweb.rest");
    }
}