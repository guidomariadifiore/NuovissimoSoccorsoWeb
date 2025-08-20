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
import webengineering.nuovissimosoccorsoweb.model.Operatore;
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
 * AGGIORNATO per gestire i nuovi requisiti:
 * - Operatori divisi in caposquadra e standard
 * - Mezzi inseriti per targa
 * - Controllo stato "Convalidata"
 * - Cambio stato richiesta da "Convalidata" ad "Attiva"
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
     * AGGIORNATO per i nuovi requisiti
     * 
     * @param missioneRequest Dati della missione da creare
     * @return Risultato della creazione
     */
    @POST
    @Secured
    public Response creaMissione(MissioneRequest missioneRequest) {
        SoccorsoDataLayer dataLayer = null;
        
        try {
            logger.info("=== CREAZIONE MISSIONE VIA REST (AGGIORNATA) ===");
            
            // Validazione input base
            if (missioneRequest == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Dati missione mancanti", "VALIDATION_ERROR"))
                        .build();
            }
            
            // Verifica ruolo admin (dal token JWT)
            String userRole = (String) requestContext.getProperty("userRole");
            if (!"admin".equals(userRole)) {
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
            
            // VALIDAZIONE SPECIFICA: Verifica che la richiesta sia in stato "Convalidata"
            RichiestaSoccorso richiesta = dataLayer.getRichiestaSoccorsoDAO()
                    .getRichiestaByCodice(missioneRequest.getRichiestaId());
            
            if (richiesta == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Richiesta non trovata", "RICHIESTA_NOT_FOUND"))
                        .build();
            }
            
            if (!"Convalidata".equals(richiesta.getStato())) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("La richiesta deve essere in stato 'Convalidata' per creare una missione. Stato attuale: " + richiesta.getStato(), "INVALID_RICHIESTA_STATE"))
                        .build();
            }
            
            // Prepara gli operatori con i ruoli corretti
            List<OperatoreConRuolo> operatoriConRuoli = preparaOperatoriConRuoli(missioneRequest);
            
            if (operatoriConRuoli.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Almeno un caposquadra è obbligatorio", "NO_CAPOSQUADRA"))
                        .build();
            }
            
            // Verifica che ci sia almeno un caposquadra
            boolean hasCaposquadra = operatoriConRuoli.stream()
                    .anyMatch(op -> "Caposquadra".equals(op.ruolo));
            
            if (!hasCaposquadra) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Almeno un caposquadra è obbligatorio", "NO_CAPOSQUADRA"))
                        .build();
            }
            
            // Prepara le targhe dei mezzi (non più ID)
            List<String> targhe = parseTargheMezzi(missioneRequest.getMezzi());
            
            // Prepara i materiali (rimangono ID)
            List<Integer> materialiIds = parseIntegerList(missioneRequest.getMateriali());
            
            // Riusa la logica del controller MVC con le modifiche
            Missione missione = creaMissioneCore(
                dataLayer,
                missioneRequest.getRichiestaId(),
                missioneRequest.getNome(),
                missioneRequest.getPosizione(),
                missioneRequest.getObiettivo(),
                operatoriConRuoli,
                targhe,
                materialiIds,
                adminId
            );
            
            // NUOVO: Cambia stato della richiesta da "Convalidata" ad "Attiva"
            dataLayer.getRichiestaSoccorsoDAO().updateStato(
                missioneRequest.getRichiestaId(), "Attiva");
            
            logger.info("Stato richiesta " + missioneRequest.getRichiestaId() + 
                       " cambiato da 'Convalidata' ad 'Attiva'");
            
            // Raccoglie email operatori per notifica
            List<String> emailOperatori = raccogliEmailOperatori(dataLayer, operatoriConRuoli);
            
            // Successo
            MissioneResponse response = new MissioneResponse(
                true,
                "Missione creata con successo. La richiesta è ora in stato 'Attiva'.",
                mapToMissioneDTO(missione),
                emailOperatori
            );
            
            logger.info("Missione creata via REST: " + missione.getNome() + 
                       " (Admin ID: " + adminId + ") - Richiesta " + 
                       missioneRequest.getRichiestaId() + " ora ATTIVA");
            
            return Response.status(Response.Status.CREATED)
                    .entity(response)
                    .build();
                    
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore nell'endpoint creazione missione", e);
            
            String errorMessage = "Errore interno del server";
            if (e.getMessage() != null) {
                errorMessage += ": " + e.getMessage();
            }
            
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse(errorMessage, "INTERNAL_ERROR"))
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
     * NUOVO METODO: Prepara la lista degli operatori con i loro ruoli.
     */
    private List<OperatoreConRuolo> preparaOperatoriConRuoli(MissioneRequest request) {
        List<OperatoreConRuolo> operatori = new ArrayList<>();
        
        // Aggiungi caposquadra
        if (request.getCaposquadra() != null && !request.getCaposquadra().trim().isEmpty()) {
            List<Integer> caposquadraIds = parseIntegerList(request.getCaposquadra());
            for (Integer id : caposquadraIds) {
                operatori.add(new OperatoreConRuolo(id, "Caposquadra"));
            }
        }
        
        // Aggiungi operatori standard
        if (request.getOperatoriStandard() != null && !request.getOperatoriStandard().trim().isEmpty()) {
            List<Integer> standardIds = parseIntegerList(request.getOperatoriStandard());
            for (Integer id : standardIds) {
                operatori.add(new OperatoreConRuolo(id, "Standard"));
            }
        }
        
        // Fallback: se non ci sono caposquadra/standard specifici, usa il campo operatori
        if (operatori.isEmpty() && request.getOperatori() != null && !request.getOperatori().trim().isEmpty()) {
            List<Integer> allIds = parseIntegerList(request.getOperatori());
            // Il primo diventa caposquadra, gli altri standard
            for (int i = 0; i < allIds.size(); i++) {
                String ruolo = (i == 0) ? "Caposquadra" : "Standard";
                operatori.add(new OperatoreConRuolo(allIds.get(i), ruolo));
            }
        }
        
        return operatori;
    }
    
    /**
     * NUOVO METODO: Prepara la lista delle targhe dei mezzi.
     */
    private List<String> parseTargheMezzi(String mezziString) {
        List<String> targhe = new ArrayList<>();
        if (mezziString != null && !mezziString.trim().isEmpty()) {
            String[] parts = mezziString.split(",");
            for (String targa : parts) {
                String targaPulita = targa.trim().toUpperCase();
                if (!targaPulita.isEmpty()) {
                    targhe.add(targaPulita);
                }
            }
        }
        return targhe;
    }
    
    /**
     * Metodo core per la creazione della missione (AGGIORNATO).
     * Versione modificata per gestire operatori con ruoli e targhe mezzi.
     */
    private Missione creaMissioneCore(SoccorsoDataLayer dataLayer, int richiestaId, 
                                    String nome, String posizione, String obiettivo,
                                    List<OperatoreConRuolo> operatoriConRuoli, 
                                    List<String> targhe, List<Integer> materialiIds, 
                                    int adminId) throws Exception {
        
        // Verifica che la richiesta esista e sia in stato "Convalidata"
        RichiestaSoccorso richiesta = dataLayer.getRichiestaSoccorsoDAO().getRichiestaByCodice(richiestaId);
        if (richiesta == null) {
            throw new IllegalArgumentException("Richiesta non trovata");
        }
        if (!"Convalidata".equals(richiesta.getStato())) {
            throw new IllegalArgumentException("La richiesta deve essere in stato 'Convalidata', stato attuale: " + richiesta.getStato());
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
            
            // 2. Assegna operatori con ruoli corretti
            for (OperatoreConRuolo operatore : operatoriConRuoli) {
                dataLayer.getMissioneDAO().assegnaOperatoreAMissione(
                    operatore.idOperatore, richiestaId, operatore.ruolo);
                logger.info("Operatore " + operatore.idOperatore + " assegnato con ruolo " + operatore.ruolo);
            }
            
            // 3. Assegna mezzi per targa (non più per ID)
            for (String targa : targhe) {
                dataLayer.getMissioneDAO().assegnaMezzoAMissione(targa, richiestaId);
                logger.info("Mezzo con targa " + targa + " assegnato alla missione");
            }
            
            // 4. Assegna materiali
            for (Integer materialeId : materialiIds) {
                dataLayer.getMissioneDAO().assegnaMaterialeAMissione(materialeId, richiestaId);
                logger.info("Materiale " + materialeId + " assegnato alla missione");
            }
            
            // Commit della transazione
            conn.commit();
            logger.info("Missione completa creata con successo per richiesta " + richiestaId);
            
            return missione;
            
        } catch (Exception ex) {
            // Rollback in caso di errore
            try {
                conn.rollback();
                logger.warning("Rollback eseguito per errore durante creazione missione");
            } catch (Exception rollbackEx) {
                logger.log(Level.SEVERE, "Errore durante rollback", rollbackEx);
            }
            throw ex;
        } finally {
            // Ripristina auto-commit
            try {
                conn.setAutoCommit(autoCommit);
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Errore ripristino auto-commit", ex);
            }
        }
    }
    
    /**
     * NUOVO METODO: Raccoglie le email degli operatori per le notifiche.
     */
    private List<String> raccogliEmailOperatori(SoccorsoDataLayer dataLayer, 
                                               List<OperatoreConRuolo> operatori) {
        List<String> email = new ArrayList<>();
        
        for (OperatoreConRuolo op : operatori) {
            try {
                Operatore operatore = dataLayer.getOperatoreDAO().getOperatoreById(op.idOperatore);
                if (operatore != null && operatore.getEmail() != null && 
                    !operatore.getEmail().trim().isEmpty()) {
                    email.add(operatore.getEmail());
                    logger.info("Email raccolta per notifica: " + operatore.getEmail() + 
                               " (Ruolo: " + op.ruolo + ")");
                }
            } catch (DataException e) {
                logger.warning("Errore raccolta email per operatore " + op.idOperatore + ": " + e.getMessage());
            }
        }
        
        return email;
    }
    
    /**
     * Lista delle richieste attive disponibili per creare missioni.
     * GET /api/missioni/richieste-attive
     */
    @GET
    @Path("richieste-attive")
    @Secured
    public Response getRichiesteAttive() {
        SoccorsoDataLayer dataLayer = null;
        
        try {
            dataLayer = createDataLayer();
            
            // Trova le richieste in stato "Convalidata" (disponibili per creare missioni)
            List<RichiestaSoccorso> richieste = dataLayer.getRichiestaSoccorsoDAO()
                    .getRichiesteByStato("Convalidata");
            
            List<RichiestaDTO> dtos = new ArrayList<>();
            for (RichiestaSoccorso r : richieste) {
                dtos.add(mapToRichiestaDTO(r));
            }
            
            return Response.ok(dtos).build();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore nel recupero richieste attive", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Errore nel recupero delle richieste", "INTERNAL_ERROR"))
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
    
    // === METODI HELPER ===
    
    /**
     * Classe helper per operatori con ruolo.
     */
    private static class OperatoreConRuolo {
        final int idOperatore;
        final String ruolo;
        
        OperatoreConRuolo(int idOperatore, String ruolo) {
            this.idOperatore = idOperatore;
            this.ruolo = ruolo;
        }
    }
    
    /**
     * Parser per liste di interi da stringhe separate da virgola.
     */
    private List<Integer> parseIntegerList(String input) {
        List<Integer> result = new ArrayList<>();
        if (input != null && !input.trim().isEmpty()) {
            String[] parts = input.split(",");
            for (String part : parts) {
                try {
                    int id = Integer.parseInt(part.trim());
                    result.add(id);
                } catch (NumberFormatException e) {
                    logger.warning("ID non valido ignorato: " + part);
                }
            }
        }
        return result;
    }
    
    /**
     * Crea il DataLayer per l'accesso al database.
     */
    /**
     * Crea il DataLayer per l'accesso al database.
     * AGGIORNATO: Aggiunge la chiamata a init() per inizializzare i DAO.
     */
    private SoccorsoDataLayer createDataLayer() throws Exception {
        try {
            InitialContext ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/soccorso");
            SoccorsoDataLayer dataLayer = new SoccorsoDataLayer(ds);
            dataLayer.init(); // QUESTO ERA MANCANTE!
            return dataLayer;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore nella creazione del DataLayer", e);
            throw new Exception("Impossibile accedere al database", e);
        }
    }
    
    /**
     * Converte Missione in MissioneDTO.
     */
    private MissioneDTO mapToMissioneDTO(Missione missione) {
        MissioneDTO dto = new MissioneDTO();
        dto.setCodiceRichiesta(missione.getCodiceRichiesta());
        dto.setNome(missione.getNome());
        dto.setPosizione(missione.getPosizione());
        dto.setObiettivo(missione.getObiettivo());
        dto.setDataOraInizio(missione.getDataOraInizio());
        dto.setIdAmministratore(missione.getIdAmministratore());
        return dto;
    }
    
    /**
     * Converte RichiestaSoccorso in RichiestaDTO.
     */
    private RichiestaDTO mapToRichiestaDTO(RichiestaSoccorso richiesta) {
        RichiestaDTO dto = new RichiestaDTO();
        dto.setCodice(richiesta.getCodice());
        dto.setStato(richiesta.getStato());
        dto.setNomeSegnalante(richiesta.getNomeSegnalante());
        dto.setEmailSegnalante(richiesta.getEmailSegnalante());
        dto.setDescrizione(richiesta.getDescrizione());
        dto.setIndirizzo(richiesta.getIndirizzo());
        dto.setCoordinate(richiesta.getCoordinate());
        return dto;
    }
}