package webengineering.nuovissimosoccorsoweb.rest.resources;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import webengineering.nuovissimosoccorsoweb.SoccorsoDataLayer;
import webengineering.nuovissimosoccorsoweb.model.RichiestaSoccorso;
import webengineering.nuovissimosoccorsoweb.rest.dto.RichiestaDTO;
import webengineering.nuovissimosoccorsoweb.rest.dto.ListaRichiesteResponse;
import webengineering.nuovissimosoccorsoweb.rest.dto.ErrorResponse;
import webengineering.nuovissimosoccorsoweb.service.RichiesteQueryService;
import webengineering.nuovissimosoccorsoweb.rest.security.Secured;
import webengineering.framework.data.DataException;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Resource REST per le operazioni di query/lettura delle richieste di soccorso.
 * Complementare al RichiesteResource esistente (che gestisce l'inserimento).
 * 
 * Gestisce:
 * - Lista paginata filtrata per stato
 * - Richieste non positive
 * - Dettagli singola richiesta
 */
@Path("richieste")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RichiesteQueryResource {
    
    private static final Logger logger = Logger.getLogger(RichiesteQueryResource.class.getName());
    
    @Context
    private HttpServletRequest httpRequest;
    
    /**
     * Lista (paginata) delle richieste di soccorso, filtrata in base alla tipologia.
     * GET /api/richieste?stato={stato}&page={page}&size={size}
     * 
     * Richiede autenticazione: solo ADMIN può vedere tutte le richieste
     * 
     * @param stato Stato delle richieste (ATTIVA, IN_CORSO, CHIUSA, IGNORATA) - opzionale
     * @param page Numero pagina (1-based, default=1)
     * @param size Elementi per pagina (default=20, max=100)
     * @return Lista paginata delle richieste
     */
    @GET
    @Secured
    public Response getListaRichieste(
            @QueryParam("stato") String stato,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        
        SoccorsoDataLayer dataLayer = null;
        
        try {
            logger.info("=== LISTA RICHIESTE PAGINATA ===");
            logger.info("Stato: " + stato + ", Pagina: " + page + ", Size: " + size);
            logger.info("Descrizione filtro: " + RichiesteQueryService.getStatoDescription(stato));
            
            // Validazione parametri di paginazione
            try {
                RichiesteQueryService.validatePaginationParams(page, size);
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Parametri di paginazione non validi", e.getMessage(), "VALIDATION_ERROR"))
                        .build();
            }
            
            // Crea DataLayer
            dataLayer = createDataLayer();
            
            // USA IL SERVICE DEDICATO - zero duplicazione!
            RichiesteQueryService.PaginatedResult<RichiestaSoccorso> result = 
                RichiesteQueryService.getRichiesteFiltrate(stato, page, size, dataLayer);
            
            // Converte i modelli interni in DTO per la risposta
            List<RichiestaDTO> richiesteDTO = new ArrayList<>();
            for (RichiestaSoccorso richiesta : result.getContent()) {
                richiesteDTO.add(mapToRichiestaDTO(richiesta));
            }
            
            // Crea risposta paginata
            ListaRichiesteResponse response = new ListaRichiesteResponse(
                richiesteDTO,
                result.getTotalElements(),
                result.getTotalPages(),
                result.getCurrentPage() - 1, // REST API usa 0-based
                result.getPageSize(),
                result.isFirst(),
                result.isLast()
            );
            
            logger.info("Restituite " + richiesteDTO.size() + " richieste su " + 
                       result.getTotalElements() + " totali");
            
            return Response.ok(response).build();
            
        } catch (DataException e) {
            logger.log(Level.SEVERE, "Errore database nel recupero lista richieste", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Errore nel database", "DATABASE_ERROR"))
                    .build();
                    
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore generico nel recupero lista richieste", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Errore di sistema", "INTERNAL_ERROR"))
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
     * Lista delle richieste di soccorso chiuse con risultato non totalmente positivo.
     * GET /api/richieste/non-positive?page={page}&size={size}
     * 
     * Richiede autenticazione: solo ADMIN
     * 
     * @param page Numero pagina (1-based, default=1) 
     * @param size Elementi per pagina (default=20, max=100)
     * @return Lista paginata delle richieste con livello di successo < 5
     */
    @GET
    @Path("non-positive")
    @Secured
    public Response getRichiesteNonPositive(
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        
        SoccorsoDataLayer dataLayer = null;
        
        try {
            logger.info("=== RICHIESTE NON POSITIVE ===");
            logger.info("Pagina: " + page + ", Size: " + size);
            
            // Validazione parametri di paginazione
            try {
                RichiesteQueryService.validatePaginationParams(page, size);
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Parametri di paginazione non validi", e.getMessage(), "VALIDATION_ERROR"))
                        .build();
            }
            
            // Crea DataLayer
            dataLayer = createDataLayer();
            
            // USA IL SERVICE DEDICATO
            RichiesteQueryService.PaginatedResult<RichiestaSoccorso> result = 
                RichiesteQueryService.getRichiesteNonPositive(page, size, dataLayer);
            
            // Converte i modelli interni in DTO per la risposta
            List<RichiestaDTO> richiesteDTO = new ArrayList<>();
            for (RichiestaSoccorso richiesta : result.getContent()) {
                richiesteDTO.add(mapToRichiestaDTO(richiesta));
            }
            
            // Crea risposta paginata
            ListaRichiesteResponse response = new ListaRichiesteResponse(
                richiesteDTO,
                result.getTotalElements(),
                result.getTotalPages(),
                result.getCurrentPage() - 1, // REST API usa 0-based
                result.getPageSize(),
                result.isFirst(),
                result.isLast()
            );
            
            logger.info("Restituite " + richiesteDTO.size() + " richieste non positive su " + 
                       result.getTotalElements() + " totali");
            
            return Response.ok(response).build();
            
        } catch (DataException e) {
            logger.log(Level.SEVERE, "Errore database nel recupero richieste non positive", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Errore nel database", "DATABASE_ERROR"))
                    .build();
                    
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore generico nel recupero richieste non positive", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Errore di sistema", "INTERNAL_ERROR"))
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
     * Dettagli di una richiesta di soccorso specifica.
     * GET /api/richieste/{id}
     * 
     * Richiede autenticazione: solo ADMIN
     * 
     * @param id ID della richiesta
     * @return Dettagli della richiesta
     */
    @GET
    @Path("{id}")
    @Secured
    public Response getDettagliRichiesta(@PathParam("id") int id) {
        SoccorsoDataLayer dataLayer = null;
        
        try {
            logger.info("=== DETTAGLI RICHIESTA ===");
            logger.info("ID richiesta: " + id);
            
            if (id <= 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("ID richiesta non valido", "VALIDATION_ERROR"))
                        .build();
            }
            
            // Crea DataLayer
            dataLayer = createDataLayer();
            
            // USA IL SERVICE DEDICATO
            RichiestaSoccorso richiesta = RichiesteQueryService.getRichiestaById(id, dataLayer);
            
            if (richiesta == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Richiesta non trovata", "NOT_FOUND"))
                        .build();
            }
            
            // Converte in DTO
            RichiestaDTO richiestaDTO = mapToRichiestaDTO(richiesta);
            
            logger.info("Restituiti dettagli per richiesta: " + richiesta.getCodice() + 
                       " - Stato: " + richiesta.getStato());
            
            return Response.ok(richiestaDTO).build();
            
        } catch (DataException e) {
            logger.log(Level.SEVERE, "Errore database nel recupero dettagli richiesta", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Errore nel database", "DATABASE_ERROR"))
                    .build();
                    
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore generico nel recupero dettagli richiesta", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Errore di sistema", "INTERNAL_ERROR"))
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
    
    // ========== METODI DI UTILITÀ ==========
    
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
     * Converte il modello interno in DTO per la risposta.
     * Riutilizza la logica del RichiesteResource esistente.
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
    }
}