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
import webengineering.nuovissimosoccorsoweb.service.ConvalidaService;

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
        
        // USA IL SERVICE CONDIVISO - zero duplicazione!
        ConvalidaService.ConvalidaResult result = 
            ConvalidaService.convalidaRichiestaByToken(token.trim(), dataLayer);
        
        if (result.isSuccess()) {
            // Successo - richiesta convalidata
            showConfirmationResult(request, response, "success", result.getMessage(), result.getRichiesta());
        } else if ("warning".equals(result.getStatus())) {
            // Warning - già convalidata
            showConfirmationResult(request, response, "warning", result.getMessage(), result.getRichiesta());
        } else {
            // Errore
            showConfirmationResult(request, response, "error", result.getMessage(), result.getRichiesta());
        }
        
    } catch (Exception ex) {
        logger.log(Level.SEVERE, "Errore generico durante conferma richiesta", ex);
        showConfirmationResult(request, response, "error", "Errore di sistema. Riprova più tardi.", null);
    }
}

// Aggiungi questo import all'inizio del file:
// import webengineering.nuovissimosoccorsoweb.service.ConvalidaService;
    
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