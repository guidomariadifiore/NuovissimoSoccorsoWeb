package webengineering.nuovissimosoccorsoweb.rest.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Utility per la gestione dei token JWT.
 * Fornisce metodi per creare, validare e estrarre informazioni dai token.
 * 
 */
public class JWTHelper {
    
    // Chiave segreta per firmare i token
    private static final String SECRET_KEY = "(UcKKX),sGf|tR?4?B5A+q-=K7*o[s6VtE7[rRkWI6jmx:[3H/D@DA1KMJ)#T=KD";
    private static final SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    
    // Durata del token: 24 ore 
    private static final long EXPIRATION_TIME = 86400000; // 24 ore in millisecondi
    
    /**
     * Genera un token JWT per l'utente.
     * 
     * @param userId ID dell'utente
     * @param email Email dell'utente (subject del token)
     * @param role Ruolo dell'utente (ADMIN, OPERATORE, UTENTE)
     * @return Token JWT firmato
     */
    public static String generateToken(Long userId, String email, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION_TIME);
        
        return Jwts.builder()
                .setSubject(email)                    // Subject (email)
                .claim("userId", userId)              // ID utente
                .claim("role", role)                  // Ruolo utente
                .setIssuedAt(now)                     // Data emissione
                .setExpiration(expiryDate)            // Data scadenza
                .signWith(key, SignatureAlgorithm.HS256) // Firma
                .compact();
    }
    
    /**
     * Versione overload per compatibilità (userId come int).
     */
    public static String createToken(int userId, String email, String role) {
        return generateToken((long) userId, email, role);
    }
    
    /**
     * Valida un token JWT e restituisce i claims se valido.
     * 
     * @param token Token da validare
     * @return Claims del token se valido, null altrimenti
     */
    public static Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            // Token non valido, scaduto, o malformato
            System.err.println("Token validation failed: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Verifica solo se il token è valido.
     * 
     * @param token Token JWT
     * @return true se valido, false altrimenti
     */
    public static boolean isTokenValid(String token) {
        return validateToken(token) != null;
    }
    
    /**
     * Estrae l'email dal token JWT (subject).
     * 
     * @param token Token JWT
     * @return Email se il token è valido, null altrimenti
     */
    public static String getEmailFromToken(String token) {
        Claims claims = validateToken(token);
        return claims != null ? claims.getSubject() : null;
    }
    
    /**
     * Estrae l'ID utente dal token JWT.
     * 
     * @param token Token JWT
     * @return ID utente se il token è valido, null altrimenti
     */
    public static Long getUserIdFromToken(String token) {
        Claims claims = validateToken(token);
        return claims != null ? claims.get("userId", Long.class) : null;
    }
    
    /**
     * Estrae il ruolo dal token JWT.
     * 
     * @param token Token JWT
     * @return Ruolo se il token è valido, null altrimenti
     */
    public static String getRoleFromToken(String token) {
        Claims claims = validateToken(token);
        return claims != null ? claims.get("role", String.class) : null;
    }
    
    /**
     * Verifica se un token è scaduto.
     * 
     * @param token Token JWT
     * @return true se il token è scaduto, false altrimenti
     */
    public static boolean isTokenExpired(String token) {
        Claims claims = validateToken(token);
        if (claims == null) return true;
        
        Date expiration = claims.getExpiration();
        return expiration.before(new Date());
    }
    
    /**
     * Estrae l'username dal token (alias per getEmailFromToken per compatibilità).
     */
    public static String getUsernameFromToken(String token) {
        return getEmailFromToken(token);
    }
}