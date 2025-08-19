package webengineering.nuovissimosoccorsoweb.rest.resources;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import webengineering.nuovissimosoccorsoweb.SoccorsoDataLayer;
import webengineering.nuovissimosoccorsoweb.model.Operatore;
import webengineering.nuovissimosoccorsoweb.model.TipoPatente;
import webengineering.nuovissimosoccorsoweb.model.Abilita;
import webengineering.nuovissimosoccorsoweb.rest.dto.OperatoreDTO;
import webengineering.nuovissimosoccorsoweb.rest.dto.ErrorResponse;
import webengineering.nuovissimosoccorsoweb.service.OperatoriQueryService;
import webengineering.nuovissimosoccorsoweb.rest.security.Secured;
import webengineering.framework.data.DataException;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Resource REST per le operazioni di query sugli operatori.
 * 
 * Gestisce:
 * - Lista operatori liberi (per la specifica)
 * - Dettagli singolo operatore
 * - Statistiche operatori
 */
@Path("operatori")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OperatoriQueryResource {
    
    private static final Logger logger = Logger.getLogger(OperatoriQueryResource.class.getName());
    
    @Context
    private HttpServletRequest httpRequest;
    
    /**
     * Lista degli operatori attualmente liberi.
     * GET /api/operatori/liberi
     * 
     * Richiede autenticazione: solo ADMIN può vedere gli operatori
     * 
     * @return Lista degli operatori disponibili (non impegnati in missioni attive)
     */
    @GET
    @Path("liberi")
    @Secured
    public Response getOperatoriLiberi() {
        SoccorsoDataLayer dataLayer = null;
        
        try {
            logger.info("=== LISTA OPERATORI LIBERI ===");
            
            // Crea DataLayer
            dataLayer = createDataLayer();
            
            // USA IL SERVICE CONDIVISO - riutilizza logica MVC!
            OperatoriQueryService.OperatoriResult result = 
                OperatoriQueryService.getOperatoriLiberi(dataLayer);
            
            if (!result.isSuccess()) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(new ErrorResponse(result.getMessage(), result.getErrorCode()))
                        .build();
            }
            
            // Converte i modelli interni in DTO per la risposta CON patenti e abilità
            List<OperatoreDTO> operatoriDTO = new ArrayList<>();
            for (Operatore operatore : result.getOperatori()) {
                operatoriDTO.add(mapToOperatoreDTOWithDetails(operatore, true, dataLayer)); // METODO AGGIORNATO
            }
            
            logger.info("Restituiti " + operatoriDTO.size() + " operatori liberi con dettagli completi");
            
            return Response.ok(operatoriDTO).build();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore generico nel recupero operatori liberi", e);
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
     * Lista di tutti gli operatori con informazioni su disponibilità.
     * GET /api/operatori?includeStato=true
     * 
     * Richiede autenticazione: solo ADMIN
     * 
     * @param includeStato Se includere informazioni su disponibilità e missioni
     * @return Lista operatori con stato dettagliato
     */
    @GET
    @Secured
    public Response getAllOperatori(@QueryParam("includeStato") @DefaultValue("false") boolean includeStato) {
        SoccorsoDataLayer dataLayer = null;
        
        try {
            logger.info("=== LISTA TUTTI OPERATORI ===");
            logger.info("Include stato: " + includeStato);
            
            // Crea DataLayer
            dataLayer = createDataLayer();
            
            if (includeStato) {
                // Lista dettagliata con stato
                List<OperatoriQueryService.OperatoreInfo> operatoriConStato = 
                    OperatoriQueryService.getAllOperatoriConStato(dataLayer);
                
                List<OperatoreDTO> operatoriDTO = new ArrayList<>();
                for (OperatoriQueryService.OperatoreInfo info : operatoriConStato) {
                    OperatoreDTO dto = mapToOperatoreDTOWithDetails(info.getOperatore(), info.isDisponibile(), dataLayer);
                    dto.setMissioniInCorso(info.getMissioniInCorso());
                    dto.setMissioniCompletate(info.getMissioniCompletate());
                    operatoriDTO.add(dto);
                }
                
                logger.info("Restituiti " + operatoriDTO.size() + " operatori con stato dettagliato");
                return Response.ok(operatoriDTO).build();
                
            } else {
                // Lista semplice - usa solo operatori liberi (per la specifica)
                OperatoriQueryService.OperatoriResult result = 
                    OperatoriQueryService.getOperatoriLiberi(dataLayer);
                
                if (!result.isSuccess()) {
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(new ErrorResponse(result.getMessage(), result.getErrorCode()))
                            .build();
                }
                
                List<OperatoreDTO> operatoriDTO = new ArrayList<>();
                for (Operatore operatore : result.getOperatori()) {
                    operatoriDTO.add(mapToOperatoreDTOWithDetails(operatore, true, dataLayer));
                }
                
                logger.info("Restituiti " + operatoriDTO.size() + " operatori liberi (lista semplice)");
                return Response.ok(operatoriDTO).build();
            }
            
        } catch (DataException e) {
            logger.log(Level.SEVERE, "Errore database nel recupero operatori", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Errore nel database", "DATABASE_ERROR"))
                    .build();
                    
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore generico nel recupero operatori", e);
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
     * Dettagli di un operatore specifico.
     * GET /api/operatori/{id}
     * 
     * Richiede autenticazione: solo ADMIN
     * 
     * @param id ID dell'operatore
     * @return Dettagli dell'operatore con stato
     */
    @GET
    @Path("{id}")
    @Secured
    public Response getDettagliOperatore(@PathParam("id") int id) {
        SoccorsoDataLayer dataLayer = null;
        
        try {
            logger.info("=== DETTAGLI OPERATORE ===");
            logger.info("ID operatore: " + id);
            
            if (id <= 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("ID operatore non valido", "VALIDATION_ERROR"))
                        .build();
            }
            
            // Crea DataLayer
            dataLayer = createDataLayer();
            
            // USA IL SERVICE CONDIVISO
            OperatoriQueryService.OperatoreInfo operatoreInfo = 
                OperatoriQueryService.getOperatoreById(id, dataLayer);
            
            if (operatoreInfo == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Operatore non trovato", "NOT_FOUND"))
                        .build();
            }
            
            // Converte in DTO dettagliato CON patenti e abilità
            OperatoreDTO operatoreDTO = mapToOperatoreDTOWithDetails(
                operatoreInfo.getOperatore(), 
                operatoreInfo.isDisponibile(),
                dataLayer
            );
            operatoreDTO.setMissioniInCorso(operatoreInfo.getMissioniInCorso());
            operatoreDTO.setMissioniCompletate(operatoreInfo.getMissioniCompletate());
            
            logger.info("Restituiti dettagli per operatore: " + operatoreInfo.getOperatore().getId() + 
                       " - " + operatoreInfo.getOperatore().getNome() + 
                       " (Disponibile: " + operatoreInfo.isDisponibile() + ")");
            
            return Response.ok(operatoreDTO).build();
            
        } catch (DataException e) {
            logger.log(Level.SEVERE, "Errore database nel recupero dettagli operatore", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Errore nel database", "DATABASE_ERROR"))
                    .build();
                    
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore generico nel recupero dettagli operatore", e);
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
     * METODO AGGIORNATO: Converte il modello interno in DTO per la risposta 
     * INCLUDENDO patenti e abilità caricate dal database.
     */
    private OperatoreDTO mapToOperatoreDTOWithDetails(Operatore operatore, boolean disponibile, SoccorsoDataLayer dataLayer) {
        OperatoreDTO dto = new OperatoreDTO();
        dto.setId(operatore.getId());
        dto.setNome(operatore.getNome());
        dto.setCognome(operatore.getCognome());
        dto.setEmail(operatore.getEmail());
        dto.setCodiceFiscale(operatore.getCodiceFiscale());
        dto.setDisponibile(disponibile);
        
        // NUOVO: Carica patenti dell'operatore
        try {
            List<TipoPatente> patentiObj = dataLayer.getOperatoreHaPatenteDAO().getPatentiByOperatore(operatore.getId());
            List<String> patentiString = new ArrayList<>();
            for (TipoPatente patente : patentiObj) {
                patentiString.add(patente.toDBString()); // Converte enum in stringa leggibile
            }
            dto.setPatenti(patentiString);
            logger.fine("Caricate " + patentiString.size() + " patenti per operatore " + operatore.getId());
        } catch (DataException e) {
            logger.log(Level.WARNING, "Errore caricamento patenti per operatore " + operatore.getId(), e);
            dto.setPatenti(new ArrayList<>()); // Lista vuota in caso di errore
        }
        
        // NUOVO: Carica abilità dell'operatore
        try {
            List<Abilita> abilitaObj = dataLayer.getAbilitaDAO().getAbilitaByOperatore(operatore.getId());
            List<String> abilitaString = new ArrayList<>();
            for (Abilita abilita : abilitaObj) {
                abilitaString.add(abilita.getTipo().toDBString()); // Converte enum in stringa leggibile
            }
            dto.setAbilita(abilitaString);
            logger.fine("Caricate " + abilitaString.size() + " abilità per operatore " + operatore.getId());
        } catch (DataException e) {
            logger.log(Level.WARNING, "Errore caricamento abilità per operatore " + operatore.getId(), e);
            dto.setAbilita(new ArrayList<>()); // Lista vuota in caso di errore
        }
        
        return dto;
    }
    
    /**
     * METODO LEGACY: Manteniamo per compatibilità, ma deprecato.
     * @deprecated Usa mapToOperatoreDTOWithDetails invece
     */
    @Deprecated
    private OperatoreDTO mapToOperatoreDTO(Operatore operatore, boolean disponibile) {
        OperatoreDTO dto = new OperatoreDTO();
        dto.setId(operatore.getId());
        dto.setNome(operatore.getNome());
        dto.setCognome(operatore.getCognome());
        dto.setEmail(operatore.getEmail());
        dto.setCodiceFiscale(operatore.getCodiceFiscale());
        dto.setDisponibile(disponibile);
        // Non include patenti e abilità - metodo deprecato
        return dto;
    }
}