package webengineering.nuovissimosoccorsoweb.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Rate limiter per limitare le richieste per IP e per email
 * Permette massimo 1 richiesta per ora per ogni IP e per ogni email
 */
public class RateLimiter {
    
    private static final Logger logger = Logger.getLogger(RateLimiter.class.getName());
    private static final long RATE_LIMIT_WINDOW_MS = 60 * 60 * 1000; // 1 ora in millisecondi
    private static final int MAX_REQUESTS_PER_WINDOW = 1;
    
    // Mappa che tiene traccia delle richieste per IP
    private final ConcurrentHashMap<String, Long> ipLastRequestTime = new ConcurrentHashMap<>();
    
    // Mappa che tiene traccia delle richieste per email
    private final ConcurrentHashMap<String, Long> emailLastRequestTime = new ConcurrentHashMap<>();
    
    // Executor per la pulizia periodica
    private final ScheduledExecutorService cleanupExecutor;
    
    // Singleton instance
    private static volatile RateLimiter instance;
    
    private RateLimiter() {
        // Avvia un task di pulizia ogni ora per rimuovere entries vecchie
        cleanupExecutor = Executors.newScheduledThreadPool(1);
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredEntries, 1, 1, TimeUnit.HOURS);
        logger.info("RateLimiter inizializzato con limite di " + MAX_REQUESTS_PER_WINDOW + " richiesta/e per ora (IP + Email)");
    }
    
    /**
     * Ottiene l'istanza singleton del RateLimiter
     */
    public static RateLimiter getInstance() {
        if (instance == null) {
            synchronized (RateLimiter.class) {
                if (instance == null) {
                    instance = new RateLimiter();
                }
            }
        }
        return instance;
    }
    
    /**
     * Verifica se un IP può fare una richiesta
     * @param ip L'indirizzo IP da verificare
     * @return true se la richiesta è permessa, false se è limitata
     */
    public synchronized boolean isIPAllowed(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            logger.warning("IP null o vuoto fornito al rate limiter");
            return false;
        }
        
        return checkAndUpdateLimit(ip, ipLastRequestTime, "IP");
    }
    
    /**
     * Verifica se un'email può fare una richiesta
     * @param email L'indirizzo email da verificare
     * @return true se la richiesta è permessa, false se è limitata
     */
    public synchronized boolean isEmailAllowed(String email) {
        if (email == null || email.trim().isEmpty()) {
            logger.warning("Email null o vuota fornita al rate limiter");
            return false;
        }
        
        String normalizedEmail = email.trim().toLowerCase();
        return checkAndUpdateLimit(normalizedEmail, emailLastRequestTime, "Email");
    }
    
    /**
     * Verifica se sia IP che email possono fare una richiesta
     * @param ip L'indirizzo IP da verificare
     * @param email L'indirizzo email da verificare
     * @return true solo se entrambi sono permessi, false se almeno uno è limitato
     */
    public synchronized boolean isAllowed(String ip, String email) {
        boolean ipAllowed = isIPAllowed(ip);
        boolean emailAllowed = isEmailAllowed(email);
        
        if (!ipAllowed && !emailAllowed) {
            logger.warning("Richiesta RIFIUTATA per IP " + ip + " e email " + email + " - entrambi limitati");
        } else if (!ipAllowed) {
            logger.warning("Richiesta RIFIUTATA per IP " + ip + " - IP limitato");
        } else if (!emailAllowed) {
            logger.warning("Richiesta RIFIUTATA per email " + email + " - email limitata");
        }
        
        return ipAllowed && emailAllowed;
    }
    
    /**
     * Metodo di compatibilità con la versione precedente
     */
    public synchronized boolean isAllowed(String ip) {
        return isIPAllowed(ip);
    }
    
    /**
     * Logica comune per verificare e aggiornare i limiti
     */
    private boolean checkAndUpdateLimit(String identifier, ConcurrentHashMap<String, Long> timeMap, String type) {
        long currentTime = System.currentTimeMillis();
        Long lastRequestTime = timeMap.get(identifier);
        
        // Se è la prima richiesta da questo identificatore, permettila
        if (lastRequestTime == null) {
            timeMap.put(identifier, currentTime);
            logger.info("Prima richiesta da " + type + " " + identifier + " - PERMESSA");
            return true;
        }
        
        // Calcola il tempo trascorso dall'ultima richiesta
        long timeSinceLastRequest = currentTime - lastRequestTime;
        
        // Se è passata almeno un'ora, permetti la richiesta
        if (timeSinceLastRequest >= RATE_LIMIT_WINDOW_MS) {
            timeMap.put(identifier, currentTime);
            logger.info("Richiesta da " + type + " " + identifier + " dopo " + (timeSinceLastRequest / 1000 / 60) + " minuti - PERMESSA");
            return true;
        }
        
        // Altrimenti, rifiuta la richiesta
        long remainingTimeMs = RATE_LIMIT_WINDOW_MS - timeSinceLastRequest;
        long remainingMinutes = remainingTimeMs / 1000 / 60;
        
        logger.warning("Richiesta da " + type + " " + identifier + " RIFIUTATA - deve attendere ancora " + remainingMinutes + " minuti");
        return false;
    }
    
    /**
     * Ottiene il tempo rimanente in minuti prima che un IP possa fare una nuova richiesta
     */
    public synchronized long getRemainingTimeMinutes(String ip) {
        return getRemainingTime(ip, ipLastRequestTime);
    }
    
    /**
     * Ottiene il tempo rimanente in minuti prima che un'email possa fare una nuova richiesta
     */
    public synchronized long getEmailRemainingTimeMinutes(String email) {
        if (email == null || email.trim().isEmpty()) {
            return 0;
        }
        String normalizedEmail = email.trim().toLowerCase();
        return getRemainingTime(normalizedEmail, emailLastRequestTime);
    }
    
    /**
     * Logica comune per calcolare il tempo rimanente
     */
    private long getRemainingTime(String identifier, ConcurrentHashMap<String, Long> timeMap) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return 0;
        }
        
        Long lastRequestTime = timeMap.get(identifier);
        if (lastRequestTime == null) {
            return 0; // Può fare subito una richiesta
        }
        
        long currentTime = System.currentTimeMillis();
        long timeSinceLastRequest = currentTime - lastRequestTime;
        
        if (timeSinceLastRequest >= RATE_LIMIT_WINDOW_MS) {
            return 0; // Può fare subito una richiesta
        }
        
        long remainingTimeMs = RATE_LIMIT_WINDOW_MS - timeSinceLastRequest;
        return remainingTimeMs / 1000 / 60; // Converti in minuti
    }
    
    /**
     * Pulisce le entries scadute dalle mappe per evitare memory leak
     */
    private void cleanupExpiredEntries() {
        long currentTime = System.currentTimeMillis();
        int removedIPs = cleanupMap(ipLastRequestTime, currentTime, "IP");
        int removedEmails = cleanupMap(emailLastRequestTime, currentTime, "Email");
        
        int totalRemoved = removedIPs + removedEmails;
        if (totalRemoved > 0) {
            logger.info("Pulizia rate limiter: rimossi " + removedIPs + " IP e " + removedEmails + " email scaduti");
        }
        
        logger.info("Rate limiter status: " + ipLastRequestTime.size() + " IP e " + emailLastRequestTime.size() + " email attualmente tracciati");
    }
    
    /**
     * Pulisce una mappa specifica
     */
    private int cleanupMap(ConcurrentHashMap<String, Long> timeMap, long currentTime, String type) {
        java.util.List<String> toRemove = new java.util.ArrayList<>();
        
        // Identifica tutte le entries più vecchie di 1 ora
        for (java.util.Map.Entry<String, Long> entry : timeMap.entrySet()) {
            if ((currentTime - entry.getValue()) > RATE_LIMIT_WINDOW_MS) {
                toRemove.add(entry.getKey());
            }
        }
        
        // Rimuovi gli identificatori scaduti
        for (String identifier : toRemove) {
            timeMap.remove(identifier);
        }
        
        return toRemove.size();
    }
    
    /**
     * Ottiene statistiche del rate limiter
     */
    public synchronized RateLimiterStats getStats() {
        return new RateLimiterStats(
            ipLastRequestTime.size(),
            emailLastRequestTime.size(),
            RATE_LIMIT_WINDOW_MS / 1000 / 60, // window in minuti
            MAX_REQUESTS_PER_WINDOW
        );
    }
    
    /**
     * Reset del rate limiter (per test o amministrazione)
     */
    public synchronized void reset() {
        ipLastRequestTime.clear();
        emailLastRequestTime.clear();
        logger.info("Rate limiter reset completato");
    }
    
    /**
     * Shutdown del rate limiter
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        logger.info("Rate limiter shutdown completato");
    }
    
    /**
     * Classe per le statistiche del rate limiter
     */
    public static class RateLimiterStats {
        private final int trackedIPs;
        private final int trackedEmails;
        private final long windowMinutes;
        private final int maxRequestsPerWindow;
        
        public RateLimiterStats(int trackedIPs, int trackedEmails, long windowMinutes, int maxRequestsPerWindow) {
            this.trackedIPs = trackedIPs;
            this.trackedEmails = trackedEmails;
            this.windowMinutes = windowMinutes;
            this.maxRequestsPerWindow = maxRequestsPerWindow;
        }
        
        public int getTrackedIPs() { return trackedIPs; }
        public int getTrackedEmails() { return trackedEmails; }
        public long getWindowMinutes() { return windowMinutes; }
        public int getMaxRequestsPerWindow() { return maxRequestsPerWindow; }
        
        @Override
        public String toString() {
            return String.format("RateLimiter Stats: %d IP e %d email tracciati, %d richieste max per %d minuti", 
                               trackedIPs, trackedEmails, maxRequestsPerWindow, windowMinutes);
        }
    }
}