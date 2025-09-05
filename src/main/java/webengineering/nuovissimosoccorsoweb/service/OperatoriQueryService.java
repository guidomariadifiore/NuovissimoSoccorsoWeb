package webengineering.nuovissimosoccorsoweb.service;

import webengineering.nuovissimosoccorsoweb.SoccorsoDataLayer;
import webengineering.nuovissimosoccorsoweb.model.Operatore;
import webengineering.framework.data.DataException;

import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Servizio per le operazioni di query sugli operatori.
 * 
 * Gestisce:
 * - Lista operatori liberi (disponibili)
 * - Lista tutti gli operatori con stato
 * - Dettagli singolo operatore
 */
public class OperatoriQueryService {
    
    private static final Logger logger = Logger.getLogger(OperatoriQueryService.class.getName());
    
    
    public static class OperatoreInfo {
        private final Operatore operatore;
        private final boolean disponibile;
        private final int missioniInCorso;
        private final int missioniCompletate;
        
        public OperatoreInfo(Operatore operatore, boolean disponibile, int missioniInCorso, int missioniCompletate) {
            this.operatore = operatore;
            this.disponibile = disponibile;
            this.missioniInCorso = missioniInCorso;
            this.missioniCompletate = missioniCompletate;
        }
        
        // Getters
        public Operatore getOperatore() { return operatore; }
        public boolean isDisponibile() { return disponibile; }
        public int getMissioniInCorso() { return missioniInCorso; }
        public int getMissioniCompletate() { return missioniCompletate; }
    }
    
    /**
     * Risultato dell'operazione di query operatori.
     */
    public static class OperatoriResult {
        private final boolean success;
        private final String message;
        private final List<Operatore> operatori;
        private final String errorCode;
        
        private OperatoriResult(boolean success, String message, List<Operatore> operatori, String errorCode) {
            this.success = success;
            this.message = message;
            this.operatori = operatori;
            this.errorCode = errorCode;
        }
        
        public static OperatoriResult success(String message, List<Operatore> operatori) {
            return new OperatoriResult(true, message, operatori, null);
        }
        
        public static OperatoriResult error(String message, String errorCode) {
            return new OperatoriResult(false, message, null, errorCode);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public List<Operatore> getOperatori() { return operatori; }
        public String getErrorCode() { return errorCode; }
    }
    
    /**
     * Recupera tutti gli operatori attualmente liberi/disponibili.
     * 
     * Usa la query complessa esistente che verifica operatori NON impegnati in missioni attive.
     * 
     * @param dataLayer DataLayer per accesso database
     * @return Risultato con lista operatori liberi
     */
    public static OperatoriResult getOperatoriLiberi(SoccorsoDataLayer dataLayer) {
        try {
            logger.info("=== RECUPERO OPERATORI LIBERI ===");
            
            List<Operatore> operatoriLiberi = dataLayer.getOperatoreDAO().getOperatoriDisponibili();
            
            logger.info("Trovati " + operatoriLiberi.size() + " operatori disponibili");
            
            // Log dettagli per debug
            for (Operatore op : operatoriLiberi) {
                logger.fine("Operatore libero: " + op.getId() + " - " + 
                           op.getNome() + " " + op.getCognome());
            }
            
            return OperatoriResult.success(
                "Operatori liberi recuperati con successo", 
                operatoriLiberi
            );
            
        } catch (DataException e) {
            logger.log(Level.SEVERE, "Errore database nel recupero operatori liberi", e);
            return OperatoriResult.error("Errore nel database", "DATABASE_ERROR");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore generico nel recupero operatori liberi", e);
            return OperatoriResult.error("Errore di sistema", "INTERNAL_ERROR");
        }
    }
    
    /**
     * Recupera tutti gli operatori con informazioni dettagliate su disponibilità.
     * 
     * @param dataLayer DataLayer per accesso database
     * @return Lista operatori con stato dettagliato
     */
    public static List<OperatoreInfo> getAllOperatoriConStato(SoccorsoDataLayer dataLayer) throws DataException {
        try {
            logger.info("=== RECUPERO TUTTI OPERATORI CON STATO ===");
            
            // 1. Carica tutti gli operatori
            List<Operatore> tuttiOperatori = dataLayer.getOperatoreDAO().getAllOperatori();
            
            // 2. Carica operatori disponibili per confronto
            List<Operatore> operatoriLiberi = dataLayer.getOperatoreDAO().getOperatoriDisponibili();
            
            // 3. Crea lista con informazioni dettagliate
            List<OperatoreInfo> operatoriConStato = new java.util.ArrayList<>();
            
            for (Operatore operatore : tuttiOperatori) {
                // Verifica se è libero
                boolean disponibile = operatoriLiberi.stream()
                    .anyMatch(libero -> libero.getId() == operatore.getId());
                
                // Conta missioni 
                int missioniInCorso = 0;
                int missioniCompletate = 0;
                
                try {
                    missioniInCorso = dataLayer.getMissioneDAO().countMissioniInCorsoByOperatore(operatore.getId());
                    missioniCompletate = dataLayer.getMissioneDAO().countMissioniCompletateByOperatore(operatore.getId());
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Errore nel conteggio missioni per operatore " + operatore.getId(), ex);
                }
                
                operatoriConStato.add(new OperatoreInfo(
                    operatore, disponibile, missioniInCorso, missioniCompletate
                ));
            }
            
            logger.info("Elaborati " + operatoriConStato.size() + " operatori con stato");
            
            return operatoriConStato;
            
        } catch (DataException e) {
            logger.log(Level.SEVERE, "Errore database nel recupero operatori con stato", e);
            throw e;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore generico nel recupero operatori con stato", e);
            throw new DataException("Errore nel recupero degli operatori", e);
        }
    }
    
    /**
     * Recupera un singolo operatore con informazioni dettagliate.
     * 
     * @param id ID dell'operatore
     * @param dataLayer DataLayer per accesso database
     * @return Informazioni dettagliate operatore o null se non trovato
     */
    public static OperatoreInfo getOperatoreById(int id, SoccorsoDataLayer dataLayer) throws DataException {
        try {
            logger.info("Recupero dettagli operatore ID: " + id);
            
            // 1. Carica operatore base
            Operatore operatore = dataLayer.getOperatoreDAO().getOperatoreById(id);
            if (operatore == null) {
                return null;
            }
            
            // 2. Verifica disponibilità
            List<Operatore> operatoriLiberi = dataLayer.getOperatoreDAO().getOperatoriDisponibili();
            boolean disponibile = operatoriLiberi.stream()
                .anyMatch(libero -> libero.getId() == id);
            
            // 3. Conta missioni
            int missioniInCorso = 0;
            int missioniCompletate = 0;
            
            try {
                missioniInCorso = dataLayer.getMissioneDAO().countMissioniInCorsoByOperatore(id);
                missioniCompletate = dataLayer.getMissioneDAO().countMissioniCompletateByOperatore(id);
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Errore nel conteggio missioni per operatore " + id, ex);
            }
            
            return new OperatoreInfo(operatore, disponibile, missioniInCorso, missioniCompletate);
            
        } catch (DataException e) {
            logger.log(Level.SEVERE, "Errore database nel recupero operatore per ID", e);
            throw e;
        }
    }
    
    /**
     * Conta gli operatori per stato.
     */
    public static class StatisticheOperatori {
        private final int totaleOperatori;
        private final int operatoriLiberi;
        private final int operatoriOccupati;
        
        public StatisticheOperatori(int totaleOperatori, int operatoriLiberi, int operatoriOccupati) {
            this.totaleOperatori = totaleOperatori;
            this.operatoriLiberi = operatoriLiberi;
            this.operatoriOccupati = operatoriOccupati;
        }
        
        // Getters
        public int getTotaleOperatori() { return totaleOperatori; }
        public int getOperatoriLiberi() { return operatoriLiberi; }
        public int getOperatoriOccupati() { return operatoriOccupati; }
    }
    
    /**
     * Calcola statistiche operatori.
     */
    public static StatisticheOperatori getStatisticheOperatori(SoccorsoDataLayer dataLayer) throws DataException {
        List<Operatore> tutti = dataLayer.getOperatoreDAO().getAllOperatori();
        List<Operatore> liberi = dataLayer.getOperatoreDAO().getOperatoriDisponibili();
        
        int totale = tutti.size();
        int disponibili = liberi.size();
        int occupati = totale - disponibili;
        
        return new StatisticheOperatori(totale, disponibili, occupati);
    }
}
