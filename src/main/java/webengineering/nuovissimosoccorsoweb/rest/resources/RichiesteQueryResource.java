package webengineering.nuovissimosoccorsoweb.rest.resources;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.container.ContainerRequestContext;
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
import webengineering.nuovissimosoccorsoweb.model.InfoMissione;
import webengineering.nuovissimosoccorsoweb.model.Missione;
import webengineering.nuovissimosoccorsoweb.rest.dto.AnnullaRichiestaResponse;

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
    
    @Context
private ContainerRequestContext requestContext;
    
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
    /**
     * Annulla una richiesta di soccorso.
     * PUT /api/richieste/{id}/annulla
     * 
     * Richiede autenticazione: solo ADMIN può annullare richieste
     * 
     * @param id ID della richiesta da annullare
     * @return Risultato dell'operazione
     */
   @PUT
    @Path("{id}/annulla")
    @Secured
    public Response annullaRichiesta(@PathParam("id") int id) {
        SoccorsoDataLayer dataLayer = null;
        
        try {
            logger.info("=== ANNULLAMENTO RICHIESTA VIA REST ===");
            logger.info("ID richiesta da annullare: " + id);
            
            // DEBUG: Stampa tutte le proprietà del context
            String userRole = (String) requestContext.getProperty("userRole");
            String username = (String) requestContext.getProperty("username");
            Integer userId = (Integer) requestContext.getProperty("userId");
            String token = (String) requestContext.getProperty("token");
            
            logger.info("DEBUG - Proprietà token per annullamento:");
            logger.info("  userRole: '" + userRole + "'");
            logger.info("  username: '" + username + "'");
            logger.info("  userId: " + userId);
            logger.info("  token presente: " + (token != null ? "SÌ" : "NO"));
            
            // Validazione input
            if (id <= 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("ID richiesta non valido", "VALIDATION_ERROR"))
                        .build();
            }
            
            // Verifica ruolo admin (dal token JWT) - CON GESTIONE CASE-INSENSITIVE
            boolean isAdmin = "ADMIN".equalsIgnoreCase(userRole) || "admin".equals(userRole) || "amministratore".equalsIgnoreCase(userRole);
            
            if (!isAdmin) {
                logger.warning("DEBUG - Accesso NEGATO per annullamento!");
                logger.warning("  Ruolo richiesto: ADMIN/admin/amministratore");
                logger.warning("  Ruolo trovato: '" + userRole + "'");
                logger.warning("  Username: " + username);
                logger.warning("  User ID: " + userId);
                
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(new ErrorResponse("Solo gli amministratori possono annullare richieste. Ruolo trovato: " + userRole, "ACCESS_DENIED"))
                        .build();
            }
            
            logger.info("DEBUG - Controllo ruolo SUPERATO per annullamento! Utente è ADMIN");
            // Ottieni ID admin dal token
            Integer adminId = (Integer) requestContext.getProperty("userId");
            if (adminId == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new ErrorResponse("ID amministratore non trovato nel token", "INVALID_TOKEN"))
                        .build();
            }
            
            logger.info("DEBUG - ID amministratore per annullamento: " + adminId);
            
            // resto del codice esistente...
            dataLayer = createDataLayer();
            
            // Verifica che la richiesta esista
            RichiestaSoccorso richiesta = dataLayer.getRichiestaSoccorsoDAO().getRichiestaByCodice(id);
            if (richiesta == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Richiesta non trovata", "NOT_FOUND"))
                        .build();
            }
            
            // Controlla se la richiesta può essere annullata
            String statoAttuale = richiesta.getStato();
            if ("Annullata".equals(statoAttuale)) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("La richiesta è già stata annullata", "ALREADY_CANCELLED"))
                        .build();
            }
            
            if ("Chiusa".equals(statoAttuale)) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Non è possibile annullare una richiesta già chiusa", "CANNOT_CANCEL_CLOSED"))
                        .build();
            }
            
            // Verifica se ci sono missioni attive collegate
            boolean hasMissioniAttive = verificaMissioniAttive(dataLayer, id);
            if (hasMissioniAttive) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Non è possibile annullare una richiesta con missioni attive. Chiudere prima le missioni.", "HAS_ACTIVE_MISSIONS"))
                        .build();
            }
            
            // Esegui l'annullamento
            String statoOriginale = richiesta.getStato();
            dataLayer.getRichiestaSoccorsoDAO().updateStato(id, "Annullata");
            
            // Ricarica la richiesta aggiornata
            richiesta = dataLayer.getRichiestaSoccorsoDAO().getRichiestaByCodice(id);
            
            // Log dell'operazione
            logger.info("Richiesta " + id + " annullata dall'amministratore " + adminId + 
                       " (stato: " + statoOriginale + " → Annullata)");
            
            // Prepara la risposta di successo
            AnnullaRichiestaResponse response = new AnnullaRichiestaResponse(
                true,
                "Richiesta annullata con successo",
                mapToRichiestaDTO(richiesta),
                statoOriginale,
                adminId
            );
            
            return Response.ok(response).build();
            
        } catch (DataException e) {
            logger.log(Level.SEVERE, "Errore database durante annullamento richiesta", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Errore nel database", "DATABASE_ERROR"))
                    .build();
                    
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore generico durante annullamento richiesta", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Errore interno del server", "INTERNAL_ERROR"))
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
     * Verifica se ci sono missioni attive collegate alla richiesta.
     * Versione semplificata che usa metodi esistenti.
     */
    private boolean verificaMissioniAttive(SoccorsoDataLayer dataLayer, int richiestaId) {
        try {
            // Usa il metodo esistente per verificare se esiste una missione per questa richiesta
            Missione missione = dataLayer.getMissioneDAO().getMissioneByCodice(richiestaId);
            
            if (missione == null) {
                // Nessuna missione = OK, può essere annullata
                return false;
            }
            
            // Se esiste una missione, controlliamo se è ancora attiva
            // Una missione è attiva se non ha data di fine nelle info_missione
            InfoMissione infoMissione = dataLayer.getInfoMissioneDAO().getInfoByCodiceMissione(richiestaId);
            
            if (infoMissione == null) {
                // Nessuna info missione = missione non chiusa = ancora attiva
                return true;
            }
            
            // Se c'è info missione, la missione è chiusa = OK per annullamento
            return false;
            
        } catch (DataException e) {
            logger.log(Level.WARNING, "Errore nella verifica missioni attive per richiesta " + richiestaId, e);
            // In caso di errore, presumiamo che ci siano missioni attive per sicurezza
            return true;
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