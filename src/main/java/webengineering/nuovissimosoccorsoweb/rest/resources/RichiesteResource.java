package webengineering.nuovissimosoccorsoweb.rest.resources;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import webengineering.nuovissimosoccorsoweb.SoccorsoDataLayer;
import webengineering.nuovissimosoccorsoweb.model.RichiestaSoccorso;
import webengineering.nuovissimosoccorsoweb.model.impl.RichiestaSoccorsoImpl;
import webengineering.nuovissimosoccorsoweb.rest.dto.RichiestaRequest;
import webengineering.nuovissimosoccorsoweb.rest.dto.RichiestaResponse;
import webengineering.nuovissimosoccorsoweb.rest.dto.RichiestaDTO;
import webengineering.framework.data.DataException;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Resource REST per la gestione delle richieste di soccorso.
 * Endpoint: /api/richieste/*
 * 
 * @author YourName
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
     * 
     * @param richiestaRequest Dati della richiesta
     * @return Richiesta creata con codice
     */
    @POST
    public Response inserisciRichiesta(RichiestaRequest richiestaRequest) {
        SoccorsoDataLayer dataLayer = null;
        
        try {
            logger.info("=== INSERIMENTO RICHIESTA SOCCORSO ===");
            
            // Validazione input
            if (richiestaRequest == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new RichiestaResponse(false, "Dati richiesta mancanti", null))
                        .build();
            }
            
            String validationError = validateRichiestaRequest(richiestaRequest);
            if (validationError != null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new RichiestaResponse(false, validationError, null))
                        .build();
            }
            
            // Crea DataLayer
            dataLayer = createDataLayer();
            
            // Crea la richiesta
            RichiestaSoccorso richiesta = new RichiestaSoccorsoImpl();
            
            // Imposta i dati dal request
            richiesta.setDescrizione(richiestaRequest.getDescrizione().trim());
            richiesta.setIndirizzo(richiestaRequest.getIndirizzo().trim());
            richiesta.setNome(richiestaRequest.getNome().trim());
            richiesta.setEmailSegnalante(richiestaRequest.getEmailSegnalante().trim());
            richiesta.setNomeSegnalante(richiestaRequest.getNomeSegnalante().trim());
            
            // Campi opzionali
            richiesta.setCoordinate(richiestaRequest.getCoordinate());
            richiesta.setFoto(richiestaRequest.getFoto());
            
            // Campi generati automaticamente dal sistema
            richiesta.setStato("Inviata"); // Stato iniziale default
            richiesta.setIp(getClientIpAddress()); // IP del client
            richiesta.setStringa(generateValidationString()); // Codice per convalida email
            richiesta.setIdAmministratore(0); // Nessun amministratore assegnato inizialmente
            
            // Salva nel database
            dataLayer.getRichiestaSoccorsoDAO().storeRichiesta(richiesta);
            
            logger.info("Richiesta inserita con successo - IP: " + richiesta.getIp() + 
                       ", Email: " + richiesta.getEmailSegnalante());
            
            // Prepara la risposta
            RichiestaResponse response = new RichiestaResponse(
                true, 
                "Richiesta di soccorso inviata con successo! Ti invieremo una email per la convalida.",
                mapToRichiestaDTO(richiesta)
            );
            
            return Response.status(Response.Status.CREATED)
                    .entity(response)
                    .build();
                    
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore nell'inserimento richiesta", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new RichiestaResponse(false, "Errore interno del server", null))
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
     * Crea DataLayer (stesso pattern di AuthService)
     */
    private SoccorsoDataLayer createDataLayer() throws Exception {
        InitialContext ctx = new InitialContext();
        DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/soccorso");
        SoccorsoDataLayer dataLayer = new SoccorsoDataLayer(ds);
        dataLayer.init();
        return dataLayer;
    }
    
    /**
     * Valida i dati della richiesta
     */
    private String validateRichiestaRequest(RichiestaRequest request) {
        if (request.getDescrizione() == null || request.getDescrizione().trim().isEmpty()) {
            return "La descrizione dell'emergenza è obbligatoria";
        }
        
        if (request.getDescrizione().trim().length() < 10) {
            return "La descrizione deve essere di almeno 10 caratteri";
        }
        
        if (request.getIndirizzo() == null || request.getIndirizzo().trim().isEmpty()) {
            return "L'indirizzo è obbligatorio";
        }
        
        if (request.getNome() == null || request.getNome().trim().isEmpty()) {
            return "Il nome è obbligatorio";
        }
        
        if (request.getEmailSegnalante() == null || request.getEmailSegnalante().trim().isEmpty()) {
            return "L'email del segnalante è obbligatoria";
        }
        
        if (!isValidEmail(request.getEmailSegnalante())) {
            return "L'email del segnalante non è valida";
        }
        
        if (request.getNomeSegnalante() == null || request.getNomeSegnalante().trim().isEmpty()) {
            return "Il nome del segnalante è obbligatorio";
        }
        
        return null; // Tutto OK
    }
    
    /**
     * Validazione email semplice
     */
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
    
    /**
     * Genera stringa di validazione univoca
     */
    private String generateValidationString() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }
    
    /**
     * Ottiene l'IP del client (gestisce proxy e load balancer)
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
     * Converte il modello interno in DTO per la risposta
     */
    private RichiestaDTO mapToRichiestaDTO(RichiestaSoccorso richiesta) {
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
    }}