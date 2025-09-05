package webengineering.nuovissimosoccorsoweb.rest.resources;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import webengineering.nuovissimosoccorsoweb.SoccorsoDataLayer;
import webengineering.nuovissimosoccorsoweb.rest.dto.RichiestaRequest;
import webengineering.nuovissimosoccorsoweb.rest.dto.RichiestaResponse;
import webengineering.nuovissimosoccorsoweb.rest.dto.RichiestaDTO;
import webengineering.nuovissimosoccorsoweb.service.RichiestaService;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.util.logging.Logger;
import java.util.logging.Level;
import webengineering.nuovissimosoccorsoweb.service.ConvalidaService;

/**
 * Resource REST per la gestione delle richieste di soccorso.
 */
@Path("richieste")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RichiesteResource {
    
    private static final Logger logger = Logger.getLogger(RichiesteResource.class.getName());
    
    @Context
    private HttpServletRequest httpRequest;
    
    /**
     * Endpoint per inserire una nuova richiesta di soccorso.
     * POST /api/richieste
     * 
     * NON richiede autenticazione
     */
    @POST
    public Response inserisciRichiesta(RichiestaRequest richiestaRequest) {
        SoccorsoDataLayer dataLayer = null;
        
        try {
            logger.info("=== INSERIMENTO RICHIESTA SOCCORSO ===");
            
            // Validazione input base
            if (richiestaRequest == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new RichiestaResponse(false, "Dati richiesta mancanti"))
                        .build();
            }
            
            // Crea DataLayer
            dataLayer = createDataLayer();
            
            // input per il service
            RichiestaService.RichiestaInput input = new RichiestaService.RichiestaInput(
                richiestaRequest.getDescrizione(),
                richiestaRequest.getIndirizzo(),
                richiestaRequest.getNome(),
                richiestaRequest.getEmailSegnalante(),
                richiestaRequest.getNomeSegnalante(),
                richiestaRequest.getCoordinate(),
                richiestaRequest.getFoto(),
                getClientIpAddress()
            );
            
            
            RichiestaService.RichiestaResult result = 
                RichiestaService.inserisciRichiesta(input, dataLayer);
            
            if (result.isSuccess()) {
                // Successo - prepara risposta
                RichiestaResponse response = new RichiestaResponse(
                    true, 
                    result.getMessage(),
                    mapToRichiestaDTO(result.getRichiesta())
                );
                
                return Response.status(Response.Status.CREATED)
                        .entity(response)
                        .build();
            } else {
                Response.Status status = getHttpStatusFromErrorCode(result.getErrorCode());
                return Response.status(status)
                        .entity(new RichiestaResponse(false, result.getMessage()))
                        .build();
            }
                    
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore nell'endpoint inserimento richiesta", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new RichiestaResponse(false, "Errore interno del server"))
                    .build();
        } finally {
            if (dataLayer != null) {
                try {
                    dataLayer.destroy();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Errore chiusura DataLayer", e);
                }
            }
        }
    }
    
    /**
     * Crea DataLayer.
     */
    private SoccorsoDataLayer createDataLayer() throws Exception {
        InitialContext ctx = new InitialContext();
        DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/soccorso");
        SoccorsoDataLayer dataLayer = new SoccorsoDataLayer(ds);
        dataLayer.init();
        return dataLayer;
    }
    
    
    private Response.Status getHttpStatusFromErrorCode(String errorCode) {
        if (errorCode == null) return Response.Status.INTERNAL_SERVER_ERROR;
        
        switch (errorCode) {
            case "VALIDATION_ERROR":
                return Response.Status.BAD_REQUEST;
            case "DATABASE_ERROR":
                return Response.Status.INTERNAL_SERVER_ERROR;
            default:
                return Response.Status.INTERNAL_SERVER_ERROR;
        }
    }
    
    /**
     * Ottiene l'IP del client
     */
    private String getClientIpAddress() {
        String xForwardedFor = httpRequest.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = httpRequest.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return httpRequest.getRemoteAddr();
    }
    
    /**
     * Converte il modello interno in DTO per la risposta.
     */
    private RichiestaDTO mapToRichiestaDTO(webengineering.nuovissimosoccorsoweb.model.RichiestaSoccorso richiesta) {
        RichiestaDTO dto = new RichiestaDTO();
        dto.setCodice(richiesta.getCodice());
        dto.setStato(richiesta.getStato());
        dto.setIndirizzo(richiesta.getIndirizzo());
        dto.setDescrizione(richiesta.getDescrizione());
        dto.setNome(richiesta.getNome());
        dto.setEmailSegnalante(richiesta.getEmailSegnalante());
        dto.setNomeSegnalante(richiesta.getNomeSegnalante());
        dto.setCoordinate(richiesta.getCoordinate());
        dto.setFoto(richiesta.getFoto());
        dto.setStringaValidazione(richiesta.getStringa());
        dto.setIdAmministratore(richiesta.getIdAmministratore() > 0 ? richiesta.getIdAmministratore() : null);
        return dto;
    }
    

/**
 * Endpoint per convalidare una richiesta di soccorso.
 * POST /api/richieste/{id}/convalida?token=... 
 * @param id ID della richiesta da convalidare
 * @param token Token di convalida (dalla email)
 * @return Risultato della convalida
 */
@POST
@Path("{id}/convalida")
public Response convalidaRichiesta(@PathParam("id") int id, @QueryParam("token") String token) {
    SoccorsoDataLayer dataLayer = null;
    
    try {
        logger.info("=== CONVALIDA RICHIESTA SOCCORSO ===");
        logger.info("ID: " + id + ", Token: " + (token != null ? token.substring(0, Math.min(token.length(), 10)) + "..." : "null"));
        
        // Validazione input base
        if (token == null || token.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ConvalidaResponse(false, "Token di convalida mancante", null))
                    .build();
        }
        
        if (id <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ConvalidaResponse(false, "ID richiesta non valido", null))
                    .build();
        }
        
        // Crea DataLayer
        dataLayer = createDataLayer();
        
        
        ConvalidaService.ConvalidaResult result = 
            ConvalidaService.convalidaRichiestaById(id, token, dataLayer);
        
        if (result.isSuccess()) {
            // Successo - richiesta convalidata
            ConvalidaResponse response = new ConvalidaResponse(
                true,
                result.getMessage(),
                mapToRichiestaDTO(result.getRichiesta())
            );
            
            return Response.ok(response).build();
            
        } else if ("warning".equals(result.getStatus())) {
            // errore - già convalidata 
            ConvalidaResponse response = new ConvalidaResponse(
                true, // success=true perché la richiesta è comunque convalidata
                result.getMessage(),
                result.getRichiesta() != null ? mapToRichiestaDTO(result.getRichiesta()) : null
            );
            
            return Response.ok(response).build();
            
        } else {
            Response.Status status = getHttpStatusFromConvalidaError(result.getErrorCode());
            
            ConvalidaResponse response = new ConvalidaResponse(
                false,
                result.getMessage(),
                null
            );
            
            return Response.status(status).entity(response).build();
        }
        
    } catch (Exception e) {
        logger.log(Level.SEVERE, "Errore nell'endpoint convalida richiesta", e);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ConvalidaResponse(false, "Errore interno del server", null))
                .build();
    } finally {
        if (dataLayer != null) {
            try {
                dataLayer.destroy();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Errore chiusura DataLayer", e);
            }
        }
    }
}

/**
 * Mappa errori di convalida a status HTTP.
 */
private Response.Status getHttpStatusFromConvalidaError(String errorCode) {
    if (errorCode == null) return Response.Status.INTERNAL_SERVER_ERROR;
    
    switch (errorCode) {
        case "INVALID_TOKEN":
        case "ID_MISMATCH":
        case "INVALID_STATE":
            return Response.Status.BAD_REQUEST;
        case "TOKEN_NOT_FOUND":
            return Response.Status.NOT_FOUND;
        case "DATABASE_ERROR":
        case "INTERNAL_ERROR":
        default:
            return Response.Status.INTERNAL_SERVER_ERROR;
    }
}

// DTO per la convalida

/**
 * DTO per la risposta di convalida.
 */
public static class ConvalidaResponse {
    private boolean success;
    private String message;
    private RichiestaDTO richiesta;
    
    public ConvalidaResponse() {}
    
    public ConvalidaResponse(boolean success, String message, RichiestaDTO richiesta) {
        this.success = success;
        this.message = message;
        this.richiesta = richiesta;
    }
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public RichiestaDTO getRichiesta() { return richiesta; }
    public void setRichiesta(RichiestaDTO richiesta) { this.richiesta = richiesta; }
}
}