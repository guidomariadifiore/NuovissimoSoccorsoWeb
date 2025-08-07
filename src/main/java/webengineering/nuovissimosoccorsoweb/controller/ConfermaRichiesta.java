package webengineering.nuovissimosoccorsoweb.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import webengineering.framework.result.TemplateManagerException;
import webengineering.nuovissimosoccorsoweb.SoccorsoDataLayer;
import webengineering.framework.data.DataException;
import webengineering.nuovissimosoccorsoweb.model.RichiestaSoccorso;

public class ConfermaRichiesta extends SoccorsoBaseController {
    
    private static final Logger logger = Logger.getLogger(ConfermaRichiesta.class.getName());
    
    @Override
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException {
        
        String token = request.getParameter("token");
        
        if (token == null || token.trim().isEmpty()) {
            // Token mancante - mostra errore
            showConfirmationResult(request, response, "error", "Token di conferma mancante o non valido.", null);
            return;
        }
        
        try {
            // Ottieni il DataLayer
            SoccorsoDataLayer dataLayer = (SoccorsoDataLayer) request.getAttribute("datalayer");
            if (dataLayer == null) {
                logger.severe("DataLayer non disponibile nella request");
                showConfirmationResult(request, response, "error", "Errore di sistema. Riprova più tardi.", null);
                return;
            }
            
            // Cerca la richiesta per token
            RichiestaSoccorso richiesta = dataLayer.getRichiestaSoccorsoDAO().getRichiestaByStringaValidazione(token.trim());
            
            if (richiesta == null) {
                // Token non trovato
                logger.warning("Tentativo di conferma con token non valido: " + token);
                showConfirmationResult(request, response, "error", "Token di conferma non valido o scaduto.", null);
                return;
            }
            
            // Verifica se la richiesta è già stata convalidata
            if ("Convalidata".equals(richiesta.getStato())) {
                logger.info("Richiesta già convalidata - Token: " + token + ", Codice: " + richiesta.getCodice());
                showConfirmationResult(request, response, "warning", "Questa richiesta è già stata confermata in precedenza.", richiesta);
                return;
            }
            
            // Verifica se la richiesta è nello stato corretto per essere convalidata
            if (!"Inviata".equals(richiesta.getStato())) {
                logger.warning("Tentativo di conferma richiesta in stato non valido: " + richiesta.getStato() + " - Token: " + token);
                showConfirmationResult(request, response, "error", "Questa richiesta non può essere confermata (stato: " + richiesta.getStato() + ").", richiesta);
                return;
            }
            
            // Aggiorna lo stato della richiesta
            updateRichiestaStato(dataLayer, richiesta.getCodice(), "Convalidata");
            
            // Log dell'operazione
            logger.info("Richiesta confermata con successo - Codice: " + richiesta.getCodice() + 
                       ", Token: " + token + ", Segnalante: " + richiesta.getNomeSegnalante());
            
            // Mostra successo
            showConfirmationResult(request, response, "success", "Richiesta confermata con successo!", richiesta);
            
        } catch (DataException ex) {
            logger.log(Level.SEVERE, "Errore database durante conferma richiesta", ex);
            showConfirmationResult(request, response, "error", "Errore nel database. Riprova più tardi.", null);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico durante conferma richiesta", ex);
            showConfirmationResult(request, response, "error", "Errore di sistema. Riprova più tardi.", null);
        }
    }
    
    // Aggiorna lo stato della richiesta nel database
    private void updateRichiestaStato(SoccorsoDataLayer dataLayer, int codiceRichiesta, String nuovoStato) 
            throws DataException {
        try {
            String updateQuery = "UPDATE richiesta_soccorso SET stato = ? WHERE codice = ?";
            try (java.sql.PreparedStatement stmt = dataLayer.getConnection().prepareStatement(updateQuery)) {
                stmt.setString(1, nuovoStato);
                stmt.setInt(2, codiceRichiesta);
                
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected == 0) {
                    throw new DataException("Nessuna richiesta aggiornata - Codice: " + codiceRichiesta);
                }
                
                logger.info("Stato richiesta aggiornato: Codice " + codiceRichiesta + " -> " + nuovoStato);
            }
        } catch (java.sql.SQLException ex) {
            throw new DataException("Errore SQL nell'aggiornamento stato richiesta", ex);
        }
    }
    
    // Mostra il risultato della conferma
    private void showConfirmationResult(HttpServletRequest request, HttpServletResponse response, 
                                       String type, String message, RichiestaSoccorso richiesta) 
            throws ServletException {
        
        try {
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("thispageurl", request.getAttribute("thispageurl"));
            dataModel.put("outline_tpl", null);
            
            // Aggiungi informazioni utente al dataModel
            addUserInfoToModel(request, dataModel);
            
            // Dati per il template
            dataModel.put("result_type", type); // "success", "error", "warning"
            dataModel.put("result_message", message);
            
            if (richiesta != null) {
                dataModel.put("richiesta_codice", richiesta.getCodice());
                dataModel.put("richiesta_nome", richiesta.getNome());
                dataModel.put("richiesta_indirizzo", richiesta.getIndirizzo());
                dataModel.put("richiesta_stato", richiesta.getStato());
            }
            
            // Informazioni per il template
            dataModel.put("page_title", "Conferma Richiesta di Soccorso");
            
            new webengineering.framework.result.TemplateResult(getServletContext())
                    .activate("conferma_richiesta.ftl.html", dataModel, response);
                    
        } catch (TemplateManagerException ex) {
            logger.log(Level.SEVERE, "Errore nel template di conferma", ex);
            handleError(ex, request, response);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore generico nel template di conferma", ex);
            handleError(ex, request, response);
        }
    }
}