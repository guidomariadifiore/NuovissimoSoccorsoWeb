package webengineering.nuovissimosoccorsoweb.rest.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.sql.SQLException;

/**
 * Mapper semplificato per gestire le eccezioni generiche.
 * 
 */
@Provider
public class AppExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        
        // Log dell'errore
        System.err.println("Exception: " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
        
        String message;
        Response.Status status;
        
        if (exception instanceof SQLException) {
            message = "Errore nel database";
            status = Response.Status.INTERNAL_SERVER_ERROR;
        } else if (exception instanceof IllegalArgumentException) {
            message = "Parametro non valido";
            status = Response.Status.BAD_REQUEST;
        } else if (exception instanceof SecurityException) {
            message = "Accesso negato";
            status = Response.Status.FORBIDDEN;
        } else {
            message = "Errore interno del server";
            status = Response.Status.INTERNAL_SERVER_ERROR;
        }
        
        // Risposta JSON semplice
        String jsonResponse = String.format(
            "{\"error\":\"%s\",\"message\":\"%s\",\"timestamp\":%d}",
            status.name(), message, System.currentTimeMillis()
        );
        
        return Response.status(status)
                .entity(jsonResponse)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}