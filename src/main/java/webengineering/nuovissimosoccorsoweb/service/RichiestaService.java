package webengineering.nuovissimosoccorsoweb.service;

import webengineering.nuovissimosoccorsoweb.SoccorsoDataLayer;
import webengineering.nuovissimosoccorsoweb.model.RichiestaSoccorso;
import webengineering.nuovissimosoccorsoweb.model.impl.RichiestaSoccorsoImpl;
import webengineering.framework.data.DataException;

import java.security.SecureRandom;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Servizio per la gestione delle richieste di soccorso.
 * Centralizza la logica business per evitare duplicazione tra MVC e REST.
 */
public class RichiestaService {
    
    private static final Logger logger = Logger.getLogger(RichiestaService.class.getName());
    
    /**
     * DTO per i dati di input della richiesta.
     */
    public static class RichiestaInput {
        private final String descrizione;
        private final String indirizzo;
        private final String nome;
        private final String emailSegnalante;
        private final String nomeSegnalante;
        private final String coordinate;
        private final String foto;
        private final String clientIp;
        
        public RichiestaInput(String descrizione, String indirizzo, String nome, 
                             String emailSegnalante, String nomeSegnalante, 
                             String coordinate, String foto, String clientIp) {
            this.descrizione = descrizione;
            this.indirizzo = indirizzo;
            this.nome = nome;
            this.emailSegnalante = emailSegnalante;
            this.nomeSegnalante = nomeSegnalante;
            this.coordinate = coordinate;
            this.foto = foto;
            this.clientIp = clientIp;
        }
        
        // Getters
        public String getDescrizione() { return descrizione; }
        public String getIndirizzo() { return indirizzo; }
        public String getNome() { return nome; }
        public String getEmailSegnalante() { return emailSegnalante; }
        public String getNomeSegnalante() { return nomeSegnalante; }
        public String getCoordinate() { return coordinate; }
        public String getFoto() { return foto; }
        public String getClientIp() { return clientIp; }
    }
    
    /**
     * Risultato dell'inserimento richiesta.
     */
    public static class RichiestaResult {
        private final boolean success;
        private final String message;
        private final RichiestaSoccorso richiesta;
        private final String errorCode;
        
        public RichiestaResult(boolean success, String message, RichiestaSoccorso richiesta, String errorCode) {
            this.success = success;
            this.message = message;
            this.richiesta = richiesta;
            this.errorCode = errorCode;
        }
        
        public static RichiestaResult success(String message, RichiestaSoccorso richiesta) {
            return new RichiestaResult(true, message, richiesta, null);
        }
        
        public static RichiestaResult error(String message, String errorCode) {
            return new RichiestaResult(false, message, null, errorCode);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public RichiestaSoccorso getRichiesta() { return richiesta; }
        public String getErrorCode() { return errorCode; }
    }
    
    /**
     * Inserisce una nuova richiesta di soccorso.
     * UNICA implementazione condivisa.
     */
    public static RichiestaResult inserisciRichiesta(RichiestaInput input, SoccorsoDataLayer dataLayer) {
        try {
            // 1. Validazioni business
            String validationError = validateRichiestaInput(input);
            if (validationError != null) {
                return RichiestaResult.error(validationError, "VALIDATION_ERROR");
            }
            
            // 2. Crea l'oggetto richiesta
            RichiestaSoccorso richiesta = createRichiestaFromInput(input);
            
            // 3. Salva nel database
            dataLayer.getRichiestaSoccorsoDAO().storeRichiesta(richiesta);
            
            // 4. Log dell'operazione
            logger.info("Richiesta inserita con successo - Codice: " + richiesta.getCodice() + 
                       ", IP: " + richiesta.getIp() + ", Email: " + richiesta.getEmailSegnalante());
            
            // 5. Risultato di successo
            return RichiestaResult.success(
                "Richiesta di soccorso inviata con successo! Ti invieremo una email per la convalida.", 
                richiesta
            );
            
        } catch (DataException e) {
            logger.log(Level.SEVERE, "Errore database nell'inserimento richiesta", e);
            return RichiestaResult.error("Errore nel salvataggio della richiesta", "DATABASE_ERROR");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore generico nell'inserimento richiesta", e);
            return RichiestaResult.error("Errore interno del server", "INTERNAL_ERROR");
        }
    }
    
    /**
     * Validazioni business della richiesta.
     */
    private static String validateRichiestaInput(RichiestaInput input) {
        if (input.getDescrizione() == null || input.getDescrizione().trim().isEmpty()) {
            return "La descrizione dell'emergenza è obbligatoria";
        }
        
        if (input.getDescrizione().trim().length() < 10) {
            return "La descrizione deve essere di almeno 10 caratteri";
        }
        
        if (input.getIndirizzo() == null || input.getIndirizzo().trim().isEmpty()) {
            return "L'indirizzo è obbligatorio";
        }
        
        if (input.getNome() == null || input.getNome().trim().isEmpty()) {
            return "Il nome è obbligatorio";
        }
        
        if (input.getEmailSegnalante() == null || input.getEmailSegnalante().trim().isEmpty()) {
            return "L'email del segnalante è obbligatoria";
        }
        
        if (!isValidEmail(input.getEmailSegnalante())) {
            return "L'email del segnalante non è valida";
        }
        
        if (input.getNomeSegnalante() == null || input.getNomeSegnalante().trim().isEmpty()) {
            return "Il nome del segnalante è obbligatorio";
        }
        
        return null; // Tutto OK
    }
    
    /**
     * Crea l'oggetto RichiestaSoccorso dai dati di input.
     */
    private static RichiestaSoccorso createRichiestaFromInput(RichiestaInput input) {
        RichiestaSoccorso richiesta = new RichiestaSoccorsoImpl();
        
        // Dati dal form
        richiesta.setDescrizione(input.getDescrizione().trim());
        richiesta.setIndirizzo(input.getIndirizzo().trim());
        richiesta.setNome(input.getNome().trim());
        richiesta.setEmailSegnalante(input.getEmailSegnalante().trim());
        richiesta.setNomeSegnalante(input.getNomeSegnalante().trim());
        
        // Campi opzionali
        richiesta.setCoordinate(input.getCoordinate() != null && !input.getCoordinate().trim().isEmpty() 
                               ? input.getCoordinate().trim() : null);
        richiesta.setFoto(input.getFoto() != null && !input.getFoto().trim().isEmpty() 
                         ? input.getFoto().trim() : null);
        
        // Campi generati automaticamente dal sistema
        richiesta.setStato("Inviata"); // Stato iniziale default
        richiesta.setIp(input.getClientIp()); // IP del client
        richiesta.setStringa(generateValidationString()); // Codice per convalida email
        richiesta.setIdAmministratore(0); // Nessun amministratore assegnato inizialmente
        
        return richiesta;
    }
    
    /**
     * Genera stringa di validazione univoca (stesso algoritmo del MVC).
     * 64 caratteri alfanumerici come in Home.java.
     */
    private static String generateValidationString() {
        SecureRandom random = new SecureRandom();
        StringBuilder token = new StringBuilder(64);
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz";
        
        for (int i = 0; i < 64; i++) {
            token.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return token.toString();
    }
    
    /**
     * Validazione email semplice.
     */
    private static boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}