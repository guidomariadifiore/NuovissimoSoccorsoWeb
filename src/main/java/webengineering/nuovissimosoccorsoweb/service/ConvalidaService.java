package webengineering.nuovissimosoccorsoweb.service;

import webengineering.nuovissimosoccorsoweb.SoccorsoDataLayer;
import webengineering.nuovissimosoccorsoweb.model.RichiestaSoccorso;
import webengineering.framework.data.DataException;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Servizio per la convalida delle richieste di soccorso.
 */
public class ConvalidaService {
    
    private static final Logger logger = Logger.getLogger(ConvalidaService.class.getName());
    
    /**
     * Risultato dell'operazione di convalida.
     */
    public static class ConvalidaResult {
        private final boolean success;
        private final String message;
        private final RichiestaSoccorso richiesta;
        private final String errorCode;
        private final String status; 
        
        private ConvalidaResult(boolean success, String message, RichiestaSoccorso richiesta, 
                               String errorCode, String status) {
            this.success = success;
            this.message = message;
            this.richiesta = richiesta;
            this.errorCode = errorCode;
            this.status = status;
        }
        
        public static ConvalidaResult success(String message, RichiestaSoccorso richiesta) {
            return new ConvalidaResult(true, message, richiesta, null, "success");
        }
        
        public static ConvalidaResult warning(String message, RichiestaSoccorso richiesta) {
            return new ConvalidaResult(false, message, richiesta, "ALREADY_VALIDATED", "warning");
        }
        
        public static ConvalidaResult error(String message, String errorCode) {
            return new ConvalidaResult(false, message, null, errorCode, "error");
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public RichiestaSoccorso getRichiesta() { return richiesta; }
        public String getErrorCode() { return errorCode; }
        public String getStatus() { return status; }
    }
    
    /**
     * Convalida una richiesta di soccorso usando il token.
     * 
     * @param token Token di convalida (dalla stringa email)
     * @param dataLayer DataLayer per accesso database
     * @return Risultato dell'operazione
     */
    public static ConvalidaResult convalidaRichiestaByToken(String token, SoccorsoDataLayer dataLayer) {
        try {
            // 1. Validazione input
            if (token == null || token.trim().isEmpty()) {
                return ConvalidaResult.error("Token di conferma mancante o non valido", "INVALID_TOKEN");
            }
            
            // 2. Cerca la richiesta per token
            RichiestaSoccorso richiesta = dataLayer.getRichiestaSoccorsoDAO()
                .getRichiestaByStringaValidazione(token.trim());
            
            if (richiesta == null) {
                logger.warning("Tentativo di conferma con token non valido: " + token);
                return ConvalidaResult.error("Token di conferma non valido o scaduto", "TOKEN_NOT_FOUND");
            }
            
            // 3. Verifica se la richiesta è già stata convalidata
            if ("Convalidata".equals(richiesta.getStato())) {
                logger.info("Richiesta già convalidata - Token: " + token + ", Codice: " + richiesta.getCodice());
                return ConvalidaResult.warning("Questa richiesta è già stata confermata in precedenza", richiesta);
            }
            
            // 4. Verifica se la richiesta è nello stato corretto per essere convalidata
            if (!"Inviata".equals(richiesta.getStato())) {
                logger.warning("Tentativo di conferma richiesta in stato non valido: " + richiesta.getStato() + 
                             " - Token: " + token);
                return ConvalidaResult.error("Questa richiesta non può essere confermata (stato: " + 
                                           richiesta.getStato() + ")", "INVALID_STATE");
            }
            
            // 5. Aggiorna lo stato della richiesta
            dataLayer.getRichiestaSoccorsoDAO().updateStato(richiesta.getCodice(), "Convalidata");
            
            // 6. Ricarica la richiesta per avere lo stato aggiornato
            richiesta = dataLayer.getRichiestaSoccorsoDAO().getRichiestaByCodice(richiesta.getCodice());
            
            // 7. Log dell'operazione
            logger.info("Richiesta confermata con successo - Codice: " + richiesta.getCodice() + 
                       ", Token: " + token + ", Segnalante: " + richiesta.getNomeSegnalante());
            
            // 8. Risultato di successo
            return ConvalidaResult.success("Richiesta confermata con successo!", richiesta);
            
        } catch (DataException e) {
            logger.log(Level.SEVERE, "Errore database durante convalida richiesta", e);
            return ConvalidaResult.error("Errore nel database. Riprova più tardi", "DATABASE_ERROR");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore generico durante convalida richiesta", e);
            return ConvalidaResult.error("Errore di sistema. Riprova più tardi", "INTERNAL_ERROR");
        }
    }
    
    /**
     * Convalida una richiesta di soccorso usando ID + token.
     * 
     * @param id ID della richiesta
     * @param token Token di convalida
     * @param dataLayer DataLayer per accesso database
     * @return Risultato dell'operazione
     */
    public static ConvalidaResult convalidaRichiestaById(int id, String token, SoccorsoDataLayer dataLayer) {
        try {
            // 1. Prima validazione standard tramite token
            ConvalidaResult result = convalidaRichiestaByToken(token, dataLayer);
            
            // 2. Se il token è valido, verifica che l'ID corrisponda
            if (result.isSuccess() || result.getStatus().equals("warning")) {
                RichiestaSoccorso richiesta = result.getRichiesta();
                if (richiesta != null && richiesta.getCodice() != id) {
                    logger.warning("ID richiesta non corrisponde al token - ID: " + id + 
                                 ", Token ID: " + richiesta.getCodice());
                    return ConvalidaResult.error("ID richiesta non corrisponde al token di convalida", "ID_MISMATCH");
                }
            }
            
            return result;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore durante convalida con ID", e);
            return ConvalidaResult.error("Errore di sistema. Riprova più tardi", "INTERNAL_ERROR");
        }
    }
}
