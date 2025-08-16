package webengineering.nuovissimosoccorsoweb.rest.resources;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.container.ContainerRequestContext;
import webengineering.nuovissimosoccorsoweb.SoccorsoDataLayer;
import webengineering.nuovissimosoccorsoweb.model.Missione;
import webengineering.nuovissimosoccorsoweb.model.RichiestaSoccorso;
import webengineering.nuovissimosoccorsoweb.model.impl.MissioneImpl;
import webengineering.nuovissimosoccorsoweb.rest.dto.MissioneDTO;
import webengineering.nuovissimosoccorsoweb.rest.dto.MissioneRequest;
import webengineering.nuovissimosoccorsoweb.rest.dto.MissioneResponse;
import webengineering.nuovissimosoccorsoweb.rest.dto.RichiestaDTO;
import webengineering.nuovissimosoccorsoweb.rest.dto.ErrorResponse;
import webengineering.nuovissimosoccorsoweb.rest.security.Secured;
import webengineering.framework.data.DataException;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Resource REST per la gestione delle missioni.
 * 
 * Gestisce:
 * - Creazione missioni (solo ADMIN)
 * - Lista richieste attive per creare missioni
 */
@Path("missioni")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MissioniResource {
    
    private static final Logger logger = Logger.getLogger(MissioniResource.class.getName());
    
    @Context
    private HttpServletRequest httpRequest;
    
    @Context
    private ContainerRequestContext requestContext;
    
    /**
     * Crea una nuova missione.
     * POST /api/missioni
     * 
     * Richiede autenticazione: solo ADMIN può creare missioni
     * 
     * @param missioneRequest Dati della missione da creare
     * @return Risultato della creazione
     */
    @POST
    @Secured
    public Response creaMissione(MissioneRequest missioneRequest) {
        SoccorsoDataLayer dataLayer = null;
        
        try {
            logger.info("=== CREAZIONE MISSIONE VIA REST ===");
            
            // Validazione input base
            if (missioneRequest == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Dati missione mancanti", "VALIDATION_ERROR"))
                        .build();
            }
            
            // Verifica ruolo admin (dal token JWT)
            String userRole = (String) requestContext.getProperty("userRole");
            if (!"ADMIN".equals(userRole)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(new ErrorResponse("Solo gli amministratori possono creare missioni", "ACCESS_DENIED"))
                        .build();
            }
            
            // Ottieni ID admin dal token
            Integer adminId = (Integer) requestContext.getProperty("userId");
            if (adminId == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new ErrorResponse("ID amministratore non trovato nel token", "INVALID_TOKEN"))
                        .build();
            }
            
            // Crea DataLayer
            dataLayer = createDataLayer();
            
            // Converti parametri da stringhe a liste di Integer
            List<Integer> operatoriIds = parseIntegerList(missioneRequest.getOperatori());
            List<Integer> mezziIds = parseIntegerList(missioneRequest.getMezzi());
            List<Integer> materialiIds = parseIntegerList(missioneRequest.getMateriali());
            
            // Riusa la logica del controller MVC
            Missione missione = creaMissioneCore(
                dataLayer,
                missioneRequest.getRichiestaId(),
                missioneRequest.getNome(),
                missioneRequest.getPosizione(),
                missioneRequest.getObiettivo(),
                operatoriIds,
                mezziIds,
                materialiIds,
                adminId
            );
            
            // Successo
            MissioneResponse response = new MissioneResponse(
                true,
                "Missione creata con successo",
                mapToMissioneDTO(missione),
                new ArrayList<>() // Lista email vuota per ora
            );
            
            logger.info("Missione creata via REST: " + missione.getNome() + 
                       " (Admin ID: " + adminId + ")");
            
            return Response.status(Response.Status.CREATED)
                    .entity(response)
                    .build();
                    
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore nell'endpoint creazione missione", e);
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
     * Lista delle richieste attive disponibili per creare missioni.
     * GET /api/missioni/richieste-attive
     * 
     * Richiede autenticazione: solo ADMIN
     * 
     * @return Lista richieste in stato ATTIVA
     */
    @GET
    @Path("richieste-attive")
    @Secured
    public Response getRichiesteAttive() {
        SoccorsoDataLayer dataLayer = null;
        
        try {
            logger.info("=== LISTA RICHIESTE ATTIVE PER MISSIONI ===");
            
            // Verifica ruolo admin
            String userRole = (String) requestContext.getProperty("userRole");
            if (!"ADMIN".equals(userRole)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(new ErrorResponse("Solo gli amministratori possono vedere le richieste", "ACCESS_DENIED"))
                        .build();
            }
            
            // Crea DataLayer
            dataLayer = createDataLayer();
            
            // Ottieni richieste attive
            List<RichiestaSoccorso> richiesteAttive = dataLayer.getRichiestaSoccorsoDAO()
                    .getRichiesteByStato("Attiva");
            
            // Converte in DTO
            List<RichiestaDTO> richiesteDTO = new ArrayList<>();
            for (RichiestaSoccorso richiesta : richiesteAttive) {
                richiesteDTO.add(mapToRichiestaDTO(richiesta));
            }
            
            logger.info("Restituite " + richiesteDTO.size() + " richieste attive");
            
            return Response.ok(richiesteDTO).build();
            
        } catch (DataException e) {
            logger.log(Level.SEVERE, "Errore database nel recupero richieste attive", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Errore nel database", "DATABASE_ERROR"))
                    .build();
                    
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore generico nel recupero richieste attive", e);
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
     * Logica core per creare una missione (condivisa tra MVC e REST).
     */
    private Missione creaMissioneCore(SoccorsoDataLayer dataLayer, int richiestaId, 
                                    String nome, String posizione, String obiettivo,
                                    List<Integer> operatoriIds, List<Integer> mezziIds, 
                                    List<Integer> materialiIds, int adminId) throws Exception {
        
        // Verifica che la richiesta esista e sia attiva
        RichiestaSoccorso richiesta = dataLayer.getRichiestaSoccorsoDAO().getRichiestaByCodice(richiestaId);
        if (richiesta == null) {
            throw new IllegalArgumentException("Richiesta non trovata");
        }
        if (!"Attiva".equals(richiesta.getStato())) {
            throw new IllegalArgumentException("La richiesta deve essere in stato ATTIVA");
        }
        
        // Avvia transazione
        Connection conn = dataLayer.getConnection();
        boolean autoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        
        try {
            // 1. Crea la missione
            Missione missione = new MissioneImpl();
            missione.setCodiceRichiesta(richiestaId);
            missione.setNome(nome);
            missione.setPosizione(posizione);
            missione.setObiettivo(obiettivo);
            missione.setNota("");
            missione.setDataOraInizio(LocalDateTime.now());
            missione.setIdAmministratore(adminId);
            missione.setVersion(1);
            
            // Salva la missione
            dataLayer.getMissioneDAO().storeMissione(missione);
            
            // 2. Assegna operatori con ruoli
            for (Integer operatoreId : operatoriIds) {
                dataLayer.getMissioneDAO().assegnaOperatoreAMissione(operatoreId, richiestaId, "Standard");
            }
            
            // 3. Assegna mezzi (per ora saltiamo questo step dato che non abbiamo getMezzoById)
            // I mezzi potrebbero essere passati già come targhe nel frontend
            // Per ora commentiamo questa sezione
            /*
            for (Integer mezzoId : mezziIds) {
                // Trova la targa del mezzo tramite ID
                Mezzo mezzo = dataLayer.getMezzoDAO().getMezzoById(mezzoId);
                if (mezzo != null) {
                    dataLayer.getMissioneDAO().assegnaMezzoAMissione(mezzo.getTarga(), richiestaId);
                }
            }
            */
            
            // 4. Assegna materiali
            for (Integer materialeId : materialiIds) {
                dataLayer.getMissioneDAO().assegnaMaterialeAMissione(materialeId, richiestaId);
            }
            
            // 5. Aggiorna stato richiesta
            richiesta.setStato("In Corso");
            dataLayer.getRichiestaSoccorsoDAO().storeRichiesta(richiesta);
            
            // Commit
            conn.commit();
            conn.setAutoCommit(autoCommit);
            
            return missione;
            
        } catch (Exception e) {
            // Rollback in caso di errore
            conn.rollback();
            conn.setAutoCommit(autoCommit);
            throw e;
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
     * Converte stringa di IDs separati da virgola in lista di Integer.
     */
    private List<Integer> parseIntegerList(String idsString) {
        List<Integer> result = new ArrayList<>();
        if (idsString != null && !idsString.trim().isEmpty()) {
            String[] ids = idsString.split(",");
            for (String id : ids) {
                try {
                    result.add(Integer.parseInt(id.trim()));
                } catch (NumberFormatException e) {
                    logger.warning("ID non valido ignorato: " + id);
                }
            }
        }
        return result;
    }
    
    /**
     * Mappa errori business a status HTTP.
     */
    private Response.Status getHttpStatusFromErrorCode(String errorCode) {
        if (errorCode == null) return Response.Status.INTERNAL_SERVER_ERROR;
        
        switch (errorCode) {
            case "VALIDATION_ERROR":
            case "RICHIESTA_NOT_ACTIVE":
                return Response.Status.BAD_REQUEST;
            case "RICHIESTA_NOT_FOUND":
                return Response.Status.NOT_FOUND;
            case "ACCESS_DENIED":
                return Response.Status.FORBIDDEN;
            case "DATABASE_ERROR":
            case "INTERNAL_ERROR":
            default:
                return Response.Status.INTERNAL_SERVER_ERROR;
        }
    }
    
    /**
     * Converte il modello interno in DTO per la risposta.
     */
    private MissioneDTO mapToMissioneDTO(Missione missione) {
        MissioneDTO dto = new MissioneDTO();
        dto.setCodiceRichiesta(missione.getCodiceRichiesta());
        dto.setNome(missione.getNome());
        dto.setPosizione(missione.getPosizione());
        dto.setObiettivo(missione.getObiettivo());
        dto.setNota(missione.getNota());
        dto.setDataOraInizio(missione.getDataOraInizio());
        dto.setIdAmministratore(missione.getIdAmministratore());
        return dto;
    }
    
    /**
     * Converte richiesta in DTO (riusa quello esistente).
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