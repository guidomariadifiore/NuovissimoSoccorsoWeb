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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import webengineering.nuovissimosoccorsoweb.model.InfoMissione;
import webengineering.nuovissimosoccorsoweb.model.Materiale;
import webengineering.nuovissimosoccorsoweb.model.Mezzo;
import webengineering.nuovissimosoccorsoweb.model.PartecipazioneSquadra;
import webengineering.nuovissimosoccorsoweb.rest.dto.DettagliMissioneDTO;
import webengineering.nuovissimosoccorsoweb.rest.dto.MaterialeAssegnatoDTO;
import webengineering.nuovissimosoccorsoweb.rest.dto.MezzoAssegnatoDTO;
import webengineering.nuovissimosoccorsoweb.rest.dto.MissioneOperatoreDTO;
import webengineering.nuovissimosoccorsoweb.rest.dto.MissioniOperatoreResponse;
import webengineering.nuovissimosoccorsoweb.rest.dto.OperatoreAssegnatoDTO;
import webengineering.nuovissimosoccorsoweb.rest.dto.ValutazioneMissioneDTO;
import webengineering.nuovissimosoccorsoweb.rest.dto.ChiusuraMissioneRequest;
import webengineering.nuovissimosoccorsoweb.rest.dto.ChiusuraMissioneResponse;
import webengineering.nuovissimosoccorsoweb.model.impl.InfoMissioneImpl;
import java.sql.Connection;

/**
 * Resource REST per la gestione delle missioni. 
 * - Operatori divisi in caposquadra e standard 
 * - Mezzi inseriti per targa 
 * - Controllo stato "Convalidata"  e cambio stato richiesta da "Convalidata" ad "Attiva" per la creazione della missione
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
     * Crea una nuova missione. POST /api/missioni
     * @param missioneRequest Dati della missione da creare
     * @return Risultato della creazione
     */
    @POST
    @Secured
    public Response creaMissione(MissioneRequest missioneRequest) {
        SoccorsoDataLayer dataLayer = null;

        try {
            logger.info("=== CREAZIONE MISSIONE VIA REST (AGGIORNATA) ===");

            // Validazione input
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

            // Prepara le targhe dei mezzi
            List<String> targhe = parseTargheMezzi(missioneRequest.getMezzi());

            // Prepara i materiali 
            List<Integer> materialiIds = parseIntegerList(missioneRequest.getMateriali());

            // DEBUG RIMUOVI
            logger.info("DEBUG: Iniziando creaMissioneCore...");

            
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

            // DEBUG RIMUOVI
            logger.info("DEBUG: creaMissioneCore completato con successo");

            // Cambia stato della richiesta da "Convalidata" ad "Attiva"
            logger.info("DEBUG: Cambiando stato richiesta..."); //DEBUG RIMUOVI
            dataLayer.getRichiestaSoccorsoDAO().updateStato(
                    missioneRequest.getRichiestaId(), "Attiva");

            logger.info("Stato richiesta " + missioneRequest.getRichiestaId()
                    + " cambiato da 'Convalidata' ad 'Attiva'");

            // Raccoglie email operatori per notifica
            //debug rimuovi
            logger.info("DEBUG: Raccogliendo email operatori...");

            List<String> emailOperatori = raccogliEmailOperatori(dataLayer, operatoriConRuoli);

            logger.info("DEBUG: Email raccolte: " + emailOperatori.size());

            //debug rimuovi
            logger.info("DEBUG: Creando DTO di risposta...");

            // Successo
            MissioneResponse response = new MissioneResponse(
                    true,
                    "Missione creata con successo. La richiesta è ora in stato 'Attiva'.",
                    mapToMissioneDTO(missione),
                    emailOperatori
            );
            //debug rimuovi
            logger.info("DEBUG: DTO creato, restituendo risposta di successo");

            logger.info("Missione creata via REST: " + missione.getNome()
                    + " (Admin ID: " + adminId + ") - Richiesta "
                    + missioneRequest.getRichiestaId() + " ora ATTIVA");

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
     * Prepara la lista degli operatori con i loro ruoli.
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
     * Prepara la lista delle targhe dei mezzi.
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
     * Metodo core per la creazione della missione.
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
        try {
            Missione missioneEsistente = dataLayer.getMissioneDAO().getMissioneByCodice(richiestaId);
            if (missioneEsistente != null) {
                throw new IllegalArgumentException("Esiste già una missione per la richiesta " + richiestaId
                        + ". Non è possibile creare multiple missioni per la stessa richiesta di soccorso.");
            }

        } catch (DataException e) {
            // Se getMissioneByCodice restituisce null o dà errore, significa che non esiste
            logger.info("Nessuna missione esistente trovata per richiesta " + richiestaId + " - OK per creare nuova missione");

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

            // 3. Assegna mezzi
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
     * Raccoglie le email degli operatori per le notifiche.
     */
    private List<String> raccogliEmailOperatori(SoccorsoDataLayer dataLayer,
            List<OperatoreConRuolo> operatori) {
        List<String> email = new ArrayList<>();

        for (OperatoreConRuolo op : operatori) {
            try {
                Operatore operatore = dataLayer.getOperatoreDAO().getOperatoreById(op.idOperatore);
                if (operatore != null && operatore.getEmail() != null
                        && !operatore.getEmail().trim().isEmpty()) {
                    email.add(operatore.getEmail());
                    logger.info("Email raccolta per notifica: " + operatore.getEmail()
                            + " (Ruolo: " + op.ruolo + ")");
                }
            } catch (DataException e) {
                logger.warning("Errore raccolta email per operatore " + op.idOperatore + ": " + e.getMessage());
            }
        }

        return email;
    }

    /**
     * Lista delle richieste attive disponibili per creare missioni. GET
     * /api/missioni/richieste-attive
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

    /**
     * Recupera i dettagli di una missione specifica. GET /api/missioni/{id}
     * @param id ID della missione
     * @return Dettagli completi della missione
     */
    @GET
    @Path("{id}")
    @Secured
    public Response getDettagliMissione(@PathParam("id") int id) {
        SoccorsoDataLayer dataLayer = null;

        try {
            logger.info("=== DETTAGLI MISSIONE " + id + " ===");

            // DEBUG: Stampa tutte le proprietà del context
            String userRole = (String) requestContext.getProperty("userRole");
            String username = (String) requestContext.getProperty("username");
            Integer userId = (Integer) requestContext.getProperty("userId");
            String token = (String) requestContext.getProperty("token");

            logger.info("DEBUG - Proprietà token per dettagli missione:");
            logger.info("  userRole: '" + userRole + "'");
            logger.info("  username: '" + username + "'");
            logger.info("  userId: " + userId);
            logger.info("  token presente: " + (token != null ? "SÌ" : "NO"));

            //  Verifica ruolo admin con controllo case-insensitive (come nell'annullamento)
            boolean isAdmin = "ADMIN".equalsIgnoreCase(userRole) || "admin".equals(userRole) || "amministratore".equalsIgnoreCase(userRole);

            if (!isAdmin) {
                logger.warning("DEBUG - Accesso NEGATO per dettagli missione!");
                logger.warning("  Ruolo richiesto: ADMIN/admin/amministratore");
                logger.warning("  Ruolo trovato: '" + userRole + "'");
                logger.warning("  Username: " + username);
                logger.warning("  User ID: " + userId);

                return Response.status(Response.Status.FORBIDDEN)
                        .entity(new ErrorResponse("Accesso negato. Solo gli admin possono visualizzare i dettagli delle missioni. Ruolo trovato: " + userRole, "ACCESS_DENIED"))
                        .build();
            }

            logger.info("DEBUG - Controllo ruolo SUPERATO per dettagli missione!");

            // Validazione parametri
            if (id <= 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("ID missione non valido", "INVALID_ID"))
                        .build();
            }

            // Crea DataLayer
            dataLayer = createDataLayer();

            // Trova la missione
            Missione missione = dataLayer.getMissioneDAO().getMissioneByCodice(id);
            if (missione == null) {
                logger.warning("Missione " + id + " non trovata");
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Missione non trovata", "NOT_FOUND"))
                        .build();
            }

            // Crea dettagli completi della missione
            DettagliMissioneDTO dettagli = creaDettagliMissioneCompleti(dataLayer, missione);

            logger.info("Dettagli missione " + id + " recuperati con successo");

            return Response.ok(dettagli).build();

        } catch (DataException e) {
            logger.log(Level.SEVERE, "Errore database nel recupero dettagli missione " + id, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Errore nel database", "DATABASE_ERROR"))
                    .build();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore generico nel recupero dettagli missione " + id, e);
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
     * Crea un DTO con tutti i dettagli di una missione. Include operatori,
     * mezzi, materiali e richiesta associata.
     */
    private DettagliMissioneDTO creaDettagliMissioneCompleti(SoccorsoDataLayer dataLayer, Missione missione) throws DataException {
        DettagliMissioneDTO dettagli = new DettagliMissioneDTO();

        // Dati base della missione
        dettagli.setId(missione.getCodiceRichiesta());
        dettagli.setNome(missione.getNome());
        dettagli.setPosizione(missione.getPosizione());
        dettagli.setObiettivo(missione.getObiettivo());
        dettagli.setNote(missione.getNota());
        if (missione.getDataOraInizio() != null) {
            dettagli.setDataOraInizio(missione.getDataOraInizio().toString());
        }
        // Data fine 
        try {
            InfoMissione infoMissione = dataLayer.getInfoMissioneDAO().getInfoByCodiceMissione(missione.getCodiceRichiesta());
            if (infoMissione != null) {
                if (infoMissione.getDataOraFine() != null) {
                    dettagli.setDataOraFine(infoMissione.getDataOraFine().toString());
                }
            } else {
                dettagli.setDataOraFine(null); // Missione ancora attiva
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Errore recupero info missione " + missione.getCodiceRichiesta(), e);
            dettagli.setDataOraFine(null);
        }
        dettagli.setIdAmministratore(missione.getIdAmministratore());
        dettagli.setRichiestaId(missione.getCodiceRichiesta());

        // Richiesta di soccorso associata
        try {
            RichiestaSoccorso richiesta = dataLayer.getRichiestaSoccorsoDAO().getRichiestaByCodice(missione.getCodiceRichiesta());
            if (richiesta != null) {
                RichiestaDTO richiestaDTO = new RichiestaDTO();
                richiestaDTO.setCodice(richiesta.getCodice());
                richiestaDTO.setDescrizione(richiesta.getDescrizione());
                richiestaDTO.setIndirizzo(richiesta.getIndirizzo());
                richiestaDTO.setStato(richiesta.getStato());
                richiestaDTO.setNomeSegnalante(richiesta.getNomeSegnalante());
                richiestaDTO.setEmailSegnalante(richiesta.getEmailSegnalante());

                dettagli.setRichiesta(richiestaDTO);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Errore recupero richiesta per missione " + missione.getCodiceRichiesta(), e);
        }

        // Operatori tramite PartecipazioneSquadra
        try {
            List<OperatoreAssegnatoDTO> operatori = new ArrayList<>();
            List<PartecipazioneSquadra> squadra = dataLayer.getMissioneDAO().getSquadraByMissione(missione.getCodiceRichiesta());

            for (PartecipazioneSquadra partecipazione : squadra) {
                try {
                    Operatore operatore = dataLayer.getOperatoreDAO().getOperatoreById(partecipazione.getIdOperatore());
                    if (operatore != null) {
                        OperatoreAssegnatoDTO opDTO = new OperatoreAssegnatoDTO();
                        opDTO.setId(operatore.getId());
                        opDTO.setNome(operatore.getNome());
                        opDTO.setCognome(operatore.getCognome());
                        opDTO.setEmail(operatore.getEmail());
                        opDTO.setRuolo(partecipazione.getRuolo().toString());
                        operatori.add(opDTO);
                    }
                } catch (Exception opEx) {
                    logger.log(Level.WARNING, "Errore caricamento operatore " + partecipazione.getIdOperatore(), opEx);
                }
            }
            dettagli.setOperatori(operatori);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Errore recupero operatori per missione " + missione.getCodiceRichiesta(), e);
            dettagli.setOperatori(new ArrayList<>());
        }
        List<MezzoAssegnatoDTO> mezzi = new ArrayList<>();
        try {
            List<Mezzo> mezziAssegnati = dataLayer.getMissioneDAO().getMezziByMissione(missione.getCodiceRichiesta());

            for (Mezzo mezzo : mezziAssegnati) {
                MezzoAssegnatoDTO mezzoDTO = new MezzoAssegnatoDTO();
                mezzoDTO.setTarga(mezzo.getTarga());
                mezzoDTO.setTipo("N/A");
                mezzoDTO.setModello(mezzo.getDescrizione() != null ? mezzo.getDescrizione() : "N/A");
                mezzi.add(mezzoDTO);
            }
            dettagli.setMezzi(mezzi);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Errore recupero mezzi per missione " + missione.getCodiceRichiesta(), e);
            dettagli.setMezzi(new ArrayList<>());
        }

        try {
            List<MaterialeAssegnatoDTO> materiali = new ArrayList<>();
            List<Materiale> materialiAssegnati = dataLayer.getMissioneDAO().getMaterialiByMissione(missione.getCodiceRichiesta());

            for (Materiale materiale : materialiAssegnati) {
                MaterialeAssegnatoDTO matDTO = new MaterialeAssegnatoDTO();
                matDTO.setId(materiale.getId());
                matDTO.setNome(materiale.getNome());
                matDTO.setQuantita(1); // Default
                materiali.add(matDTO);
            }
            dettagli.setMateriali(materiali);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Errore recupero materiali per missione " + missione.getCodiceRichiesta(), e);
            dettagli.setMateriali(new ArrayList<>());
        }

        // Informazioni di valutazione (se la missione è stata conclusa)
        try {
            InfoMissione infoMissione = dataLayer.getInfoMissioneDAO().getInfoByCodiceMissione(missione.getCodiceRichiesta());
            if (infoMissione != null) {
                ValutazioneMissioneDTO valutazione = new ValutazioneMissioneDTO();
                valutazione.setSuccesso(infoMissione.getSuccesso());
                valutazione.setCommento(infoMissione.getCommento());
                valutazione.setDataOraFine(infoMissione.getDataOraFine().toString());
                dettagli.setValutazione(valutazione);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Errore recupero valutazione per missione " + missione.getCodiceRichiesta(), e);
        }
        return dettagli;
    }

    /**
     * Lista delle missioni in cui un operatore è stato coinvolto. GET
     * /api/missioni/operatore/{idOperatore}
     * @param idOperatore ID dell'operatore
     * @return Lista delle missioni con dettagli
     */
    @GET
    @Path("operatore/{idOperatore}")
    @Secured
    public Response getMissioniOperatore(@PathParam("idOperatore") int idOperatore) {
        SoccorsoDataLayer dataLayer = null;

        try {
            logger.info("=== LISTA MISSIONI OPERATORE " + idOperatore + " ===");

            // Verifica ruolo admin (dal token JWT)
            String userRole = (String) requestContext.getProperty("userRole");
            if (!"ADMIN".equals(userRole) && !"admin".equals(userRole)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(new ErrorResponse("Accesso negato. Solo gli admin possono vedere le missioni degli operatori.", "ACCESS_DENIED"))
                        .build();
            }

            // Validazione input
            if (idOperatore <= 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("ID operatore non valido", "VALIDATION_ERROR"))
                        .build();
            }

            // Ottieni il DataLayer
            dataLayer = createDataLayer();

            // Verifica che l'operatore esista
            Operatore operatore = dataLayer.getOperatoreDAO().getOperatoreById(idOperatore);
            if (operatore == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Operatore non trovato", "OPERATORE_NOT_FOUND"))
                        .build();
            }

            
            List<Missione> missioni = dataLayer.getMissioneDAO().getMissioniByOperatore(idOperatore);

            // Converte in DTO con dettagli aggiuntivi
            List<MissioneOperatoreDTO> missioniDTO = new ArrayList<>();
            for (Missione missione : missioni) {
                missioniDTO.add(creaMissioneOperatoreDTO(dataLayer, missione, idOperatore));
            }

            // Crea risposta
            MissioniOperatoreResponse response = new MissioniOperatoreResponse(
                    operatore.getId(),
                    operatore.getNome() + " " + operatore.getCognome(),
                    operatore.getEmail(),
                    missioniDTO,
                    missioniDTO.size()
            );

            logger.info("Restituite " + missioniDTO.size() + " missioni per operatore "
                    + operatore.getNome() + " " + operatore.getCognome());

            return Response.ok(response).build();

        } catch (DataException ex) {
            logger.log(Level.SEVERE, "Errore database recupero missioni operatore " + idOperatore, ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Errore nel database", "DATABASE_ERROR"))
                    .build();

        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico recupero missioni operatore " + idOperatore, ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Errore interno del server", "INTERNAL_ERROR"))
                    .build();

        } finally {
            if (dataLayer != null) {
                dataLayer.destroy();
            }
        }
    }
@PUT
    @Path("{id}/chiudi")
    @Secured
    public Response chiudiMissione(@PathParam("id") int id, ChiusuraMissioneRequest richiestaChiusura) {
        SoccorsoDataLayer dataLayer = null;

        try {
            logger.info("=== CHIUSURA MISSIONE " + id + " ===");
            
            // DEBUG: Stampa le proprietà del context
            String userRole = (String) requestContext.getProperty("userRole");
            String username = (String) requestContext.getProperty("username");
            Integer userId = (Integer) requestContext.getProperty("userId");

            logger.info("DEBUG - Proprietà token per chiusura missione:");
            logger.info("  userRole: '" + userRole + "'");
            logger.info("  username: '" + username + "'");
            logger.info("  userId: " + userId);

            // Verifica ruolo admin (stesso controllo degli altri endpoint)
            boolean isAdmin = "ADMIN".equalsIgnoreCase(userRole) || "admin".equals(userRole) || "amministratore".equalsIgnoreCase(userRole);

            if (!isAdmin) {
                logger.warning("DEBUG - Accesso NEGATO per chiusura missione!");
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(new ErrorResponse("Solo gli amministratori possono chiudere le missioni. Ruolo trovato: " + userRole, "ACCESS_DENIED"))
                        .build();
            }

            // Validazione input
            if (id <= 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("ID missione non valido", "INVALID_ID"))
                        .build();
            }
            
            if (richiestaChiusura == null || !richiestaChiusura.isValid()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Dati di chiusura non validi. Livello successo deve essere 1-5 e commento obbligatorio", "VALIDATION_ERROR"))
                        .build();
            }

            // Inizializza DataLayer
            dataLayer = createDataLayer();

            // 1. Verifica che la missione esista
            Missione missione = dataLayer.getMissioneDAO().getMissioneByCodice(id);
            if (missione == null) {
                logger.warning("Missione " + id + " non trovata");
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Missione non trovata", "NOT_FOUND"))
                        .build();
            }

            // 2. Verifica che la missione sia in stato "Attiva"
            RichiestaSoccorso richiesta = dataLayer.getRichiestaSoccorsoDAO().getRichiestaByCodice(id);
            if (richiesta == null || !"Attiva".equals(richiesta.getStato())) {
                String statoAttuale = richiesta != null ? richiesta.getStato() : "Sconosciuto";
                logger.warning("Tentativo di chiudere missione " + id + " con stato non attivo: " + statoAttuale);
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("La missione deve essere in stato 'Attiva' per essere chiusa. Stato attuale: " + statoAttuale, "INVALID_STATE"))
                        .build();
            }

            // 3. Verifica che non esista già un resoconto
            InfoMissione infoEsistente = dataLayer.getInfoMissioneDAO().getInfoByCodiceMissione(id);
            if (infoEsistente != null) {
                logger.warning("Tentativo di chiudere missione " + id + " già chiusa");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("La missione è già stata chiusa", "ALREADY_CLOSED"))
                        .build();
            }

            // 4. Transazione per chiusura completa
            Connection conn = dataLayer.getConnection();
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try {
                LocalDateTime timestampChiusura = LocalDateTime.now();
                
                // 5. Crea resoconto finale (InfoMissione)
                InfoMissione infoMissione = new InfoMissioneImpl();
                infoMissione.setCodiceMissione(id);
                infoMissione.setSuccesso(richiestaChiusura.getLivelloSuccesso());
                infoMissione.setCommento(richiestaChiusura.getCommento());
                infoMissione.setDataOraFine(timestampChiusura);
                
                dataLayer.getInfoMissioneDAO().storeInfoMissione(infoMissione);
                
                // 6. Aggiorna stato richiesta a "Chiusa"
                dataLayer.getRichiestaSoccorsoDAO().updateStato(id, "Chiusa");

                // 7. Commit transazione
                conn.commit();
                logger.info("Missione " + id + " chiusa con successo. Livello successo: " + richiestaChiusura.getLivelloSuccesso());

                // 8. Risposta di successo
                ChiusuraMissioneResponse response = ChiusuraMissioneResponse.success(
                    "Missione chiusa con successo! Livello di successo: " + richiestaChiusura.getLivelloSuccesso() + "/5",
                    timestampChiusura.toString(),
                    id
                );

                return Response.ok(response).build();

            } catch (Exception e) {
                // Rollback in caso di errore
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }

        } catch (DataException e) {
            logger.log(Level.SEVERE, "Errore database nella chiusura missione " + id, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Errore nel database durante la chiusura", "DATABASE_ERROR"))
                    .build();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore generico nella chiusura missione " + id, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Errore di sistema durante la chiusura", "INTERNAL_ERROR"))
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
     * Crea un DTO con le informazioni di una missione per un operatore
     * specifico.
     */
    private MissioneOperatoreDTO creaMissioneOperatoreDTO(SoccorsoDataLayer dataLayer, Missione missione, int idOperatore) throws DataException {
        MissioneOperatoreDTO dto = new MissioneOperatoreDTO();

        // Dati base della missione
        dto.setId(missione.getCodiceRichiesta());
        dto.setNome(missione.getNome());
        dto.setPosizione(missione.getPosizione());
        dto.setObiettivo(missione.getObiettivo());
        dto.setDataOraInizio(missione.getDataOraInizio().toString());

        // Trova il ruolo dell'operatore in questa missione
        try {
            List<PartecipazioneSquadra> squadra = dataLayer.getMissioneDAO().getSquadraByMissione(missione.getCodiceRichiesta());
            String ruolo = "Standard"; // Default

            for (PartecipazioneSquadra partecipazione : squadra) {
                if (partecipazione.getIdOperatore() == idOperatore) {
                    ruolo = partecipazione.getRuolo().toString();
                    break;
                }
            }
            dto.setRuoloOperatore(ruolo);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Errore recupero ruolo operatore " + idOperatore + " in missione " + missione.getCodiceRichiesta(), e);
            dto.setRuoloOperatore("Standard");
        }

        // Stato della missione (conclusa o attiva)
        try {
            InfoMissione infoMissione = dataLayer.getInfoMissioneDAO().getInfoByCodiceMissione(missione.getCodiceRichiesta());
            if (infoMissione != null) {
                dto.setStato("CONCLUSA");
                dto.setDataOraFine(infoMissione.getDataOraFine().toString());
                dto.setLivelloSuccesso(infoMissione.getSuccesso());
            } else {
                dto.setStato("ATTIVA");
                dto.setDataOraFine(null);
                dto.setLivelloSuccesso(0);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Errore recupero info missione " + missione.getCodiceRichiesta(), e);
            dto.setStato("ATTIVA");
            dto.setDataOraFine(null);
            dto.setLivelloSuccesso(0);
        }

        // Informazioni richiesta associata
        try {
            RichiestaSoccorso richiesta = dataLayer.getRichiestaSoccorsoDAO().getRichiestaByCodice(missione.getCodiceRichiesta());
            if (richiesta != null) {
                dto.setDescrizioneRichiesta(richiesta.getDescrizione());
                dto.setIndirizzoIntervento(richiesta.getIndirizzo());
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Errore recupero richiesta per missione " + missione.getCodiceRichiesta(), e);
        }

        return dto;
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
    private SoccorsoDataLayer createDataLayer() throws Exception {
        try {
            InitialContext ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/soccorso");
            SoccorsoDataLayer dataLayer = new SoccorsoDataLayer(ds);
            dataLayer.init();
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
        dto.setNota(missione.getNota()); 

        // Converte LocalDateTime in String
        if (missione.getDataOraInizio() != null) {
            dto.setDataOraInizio(missione.getDataOraInizio().toString());
        } else {
            dto.setDataOraInizio(null);
        }

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
