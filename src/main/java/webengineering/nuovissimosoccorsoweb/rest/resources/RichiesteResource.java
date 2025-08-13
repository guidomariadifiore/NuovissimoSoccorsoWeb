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

/**
 * Resource REST per la gestione delle richieste di soccorso.
 * Ora usa il RichiestaService condiviso - zero duplicazione!
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
     * NON richiede autenticazione (in emergenza non c'è tempo per il login!)
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
            
            // Prepara input per il service
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
            
            // USA IL SERVICE CONDIVISO - zero duplicazione di logica business!
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
                // Errore di business - determina status HTTP
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
    
    /**
     * Mappa errori business a status HTTP.
     */
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
     * Ottiene l'IP del client (gestisce proxy e load balancer).
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
}