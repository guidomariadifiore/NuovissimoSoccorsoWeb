package webengineering.nuovissimosoccorsoweb.service;

import webengineering.nuovissimosoccorsoweb.SoccorsoDataLayer;
import webengineering.nuovissimosoccorsoweb.model.RichiestaSoccorso;
import webengineering.framework.data.DataException;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Servizio per le operazioni di query/lettura delle richieste di soccorso.
 * Complementare al RichiestaService esistente (che gestisce l'inserimento).
 *
 * Centralizza la logica business per evitare duplicazione tra MVC e REST.
 */
public class RichiesteQueryService {

    private static final Logger logger = Logger.getLogger(RichiesteQueryService.class.getName());

    /**
     * Risultato paginato per le richieste.
     */
    public static class PaginatedResult<T> {

        private final List<T> content;
        private final int totalElements;
        private final int totalPages;
        private final int currentPage;
        private final int pageSize;
        private final boolean first;
        private final boolean last;

        public PaginatedResult(List<T> content, int totalElements, int currentPage, int pageSize) {
            this.content = content;
            this.totalElements = totalElements;
            this.pageSize = pageSize;
            this.currentPage = currentPage;
            this.totalPages = pageSize > 0 ? (int) Math.ceil((double) totalElements / pageSize) : 0;
            this.first = currentPage == 1;
            this.last = currentPage >= totalPages || totalPages == 0;
        }

        // Getters
        public List<T> getContent() {
            return content;
        }

        public int getTotalElements() {
            return totalElements;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public int getCurrentPage() {
            return currentPage;
        }

        public int getPageSize() {
            return pageSize;
        }

        public boolean isFirst() {
            return first;
        }

        public boolean isLast() {
            return last;
        }
    }

    /**
     * Recupera richieste filtrate per stato con paginazione.
     *
     * @param stato Stato delle richieste (null per tutte)
     * @param page Numero pagina (1-based)
     * @param size Elementi per pagina
     * @param dataLayer DataLayer per accesso database
     * @return Risultato paginato
     */
    public static PaginatedResult<RichiestaSoccorso> getRichiesteFiltrate(
            String stato, int page, int size, SoccorsoDataLayer dataLayer) throws DataException {

        try {
            logger.info("Recupero richieste filtrate - Stato: " + stato + ", Pagina: " + page + ", Size: " + size);

            // Mappa stati REST a stati database se necessario
            String statoDb = (stato != null && !stato.trim().isEmpty()) ? mapStatoRestToDb(stato) : null;

            // 1. Recupera richieste filtrate (usa i metodi DAO esistenti)
            List<RichiestaSoccorso> tutteRichieste;
            if (statoDb != null) {
                tutteRichieste = dataLayer.getRichiestaSoccorsoDAO().getRichiesteByStato(statoDb);
            } else {
                tutteRichieste = dataLayer.getRichiestaSoccorsoDAO().getAllRichieste();
            }

            // ✅ NUOVA LOGICA: Filtra automaticamente le richieste "Inviata"
            List<RichiestaSoccorso> richiesteFiltrate = new ArrayList<>();
            for (RichiestaSoccorso richiesta : tutteRichieste) {
                // Escludi le richieste con stato "Inviata"
                if (!"Inviata".equals(richiesta.getStato())) {
                    richiesteFiltrate.add(richiesta);
                }
            }

            logger.info("Richieste prima del filtro 'Inviata': " + tutteRichieste.size()
                    + ", dopo il filtro: " + richiesteFiltrate.size());

            // 2. Applica paginazione in memoria
            int totalElements = richiesteFiltrate.size();
            int startIndex = (page - 1) * size;
            int endIndex = Math.min(startIndex + size, totalElements);

            List<RichiestaSoccorso> richiestePagina;
            if (startIndex >= totalElements || startIndex < 0) {
                richiestePagina = new ArrayList<>();
            } else {
                richiestePagina = richiesteFiltrate.subList(startIndex, endIndex);
            }

            logger.info("Trovate " + totalElements + " richieste totali (senza 'Inviata'), "
                    + richiestePagina.size() + " nella pagina " + page);

            return new PaginatedResult<>(richiestePagina, totalElements, page, size);

        } catch (DataException e) {
            logger.log(Level.SEVERE, "Errore database nel recupero richieste filtrate", e);
            throw e;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore generico nel recupero richieste filtrate", e);
            throw new DataException("Errore nel recupero delle richieste", e);
        }
    }

    /**
     * Recupera richieste con livello di successo < 5
     *
     * @param page Numero pagina (1-based)
     * @param size Elementi per pagina
     * @param dataLayer DataLayer per accesso database
     * @return Risultato paginato
     */
    public static PaginatedResult<RichiestaSoccorso> getRichiesteNonPositive(
            int page, int size, SoccorsoDataLayer dataLayer) throws DataException {

        try {
            logger.info("Recupero richieste non positive - Pagina: " + page + ", Size: " + size);

            // 1. Recupera richieste chiuse (usa metodo DAO esistente)
            List<RichiestaSoccorso> richiesteChiuse
                    = dataLayer.getRichiestaSoccorsoDAO().getRichiesteByStato("Chiusa");

            // 2. Per ora restituisci tutte le richieste chiuse
            // TODO: Implementare il filtro per livello di successo < 5 quando avrai il JOIN con info_missione
            // Puoi estendere il DAO con un metodo specifico o fare un JOIN manuale
            List<RichiestaSoccorso> richiesteNonPositive = richiesteChiuse;

            // 3. Applica paginazione
            int totalElements = richiesteNonPositive.size();
            int startIndex = (page - 1) * size;
            int endIndex = Math.min(startIndex + size, totalElements);

            List<RichiestaSoccorso> richiestePagina;
            if (startIndex >= totalElements || startIndex < 0) {
                richiestePagina = new ArrayList<>();
            } else {
                richiestePagina = richiesteNonPositive.subList(startIndex, endIndex);
            }

            logger.info("Trovate " + totalElements + " richieste non positive, "
                    + richiestePagina.size() + " nella pagina " + page);

            return new PaginatedResult<>(richiestePagina, totalElements, page, size);

        } catch (DataException e) {
            logger.log(Level.SEVERE, "Errore database nel recupero richieste non positive", e);
            throw e;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore generico nel recupero richieste non positive", e);
            throw new DataException("Errore nel recupero delle richieste non positive", e);
        }
    }

    /**
     * Recupera una singola richiesta per codice. Metodo di utilità per evitare
     * duplicazione.
     *
     * @param codice Codice della richiesta
     * @param dataLayer DataLayer per accesso database
     * @return Richiesta trovata o null
     */
    public static RichiestaSoccorso getRichiestaById(int codice, SoccorsoDataLayer dataLayer) throws DataException {
        try {
            logger.info("Recupero richiesta per ID: " + codice);
            return dataLayer.getRichiestaSoccorsoDAO().getRichiestaByCodice(codice);
        } catch (DataException e) {
            logger.log(Level.SEVERE, "Errore database nel recupero richiesta per ID", e);
            throw e;
        }
    }

    /**
     * Valida i parametri di paginazione.
     */
    public static void validatePaginationParams(int page, int size) {
        if (page < 1) {
            throw new IllegalArgumentException("Il numero di pagina deve essere >= 1");
        }
        if (size < 1) {
            throw new IllegalArgumentException("La dimensione della pagina deve essere >= 1");
        }
        if (size > 100) {
            throw new IllegalArgumentException("La dimensione della pagina non può essere > 100");
        }
    }

    /**
     * Mappa gli stati REST agli stati del database. Centralizza la logica di
     * mappatura per evitare duplicazione.
     */
    public static String mapStatoRestToDb(String statoRest) {
        if (statoRest == null) {
            return null;
        }

        switch (statoRest.toUpperCase()) {
            case "ATTIVA":
            case "ATTIVE":
                return "Attiva";
            case "IN_CORSO":
            case "INCORSO":
                return "In corso";
            case "CHIUSA":
            case "CHIUSE":
                return "Chiusa";
            case "IGNORATA":
            case "IGNORATE":
                return "Ignorata";
            case "CONVALIDATA":
            case "CONVALIDATE":
                return "Convalidata";
            case "ANNULLATA":
            case "ANNULLATE":
                return "Annullata";
            default:
                // Restituisce il valore originale se non trovato nella mappatura
                return statoRest;
        }
    }

    /**
     * Ottiene una descrizione user-friendly dello stato. Utile per logging e
     * messaggi.
     */
    public static String getStatoDescription(String stato) {
        if (stato == null) {
            return "Tutti gli stati";
        }

        switch (stato.toUpperCase()) {
            case "ATTIVA":
                return "Richieste attive";
            case "IN_CORSO":
                return "Richieste in corso";
            case "CHIUSA":
                return "Richieste chiuse";
            case "IGNORATA":
                return "Richieste ignorate";
            case "CONVALIDATA":
                return "Richieste convalidate";
            case "ANNULLATA":
                return "Richieste annullate";
            default:
                return "Richieste con stato: " + stato;
        }
    }
}
