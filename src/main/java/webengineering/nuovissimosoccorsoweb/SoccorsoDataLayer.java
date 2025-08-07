package webengineering.nuovissimosoccorsoweb;

import webengineering.nuovissimosoccorsoweb.dao.impl.RichiestaSoccorsoDAO_MySQL;
import webengineering.nuovissimosoccorsoweb.dao.impl.*;
import webengineering.nuovissimosoccorsoweb.dao.*;
import webengineering.nuovissimosoccorsoweb.model.*;

import webengineering.framework.data.DataException;
import webengineering.framework.data.DataLayer;

import java.sql.SQLException;
import javax.sql.DataSource;

public class SoccorsoDataLayer extends DataLayer {

    public SoccorsoDataLayer(DataSource datasource) throws SQLException {
        super(datasource);
    }

    @Override
    public void init() throws DataException {
        registerDAO(Abilita.class, new AbilitaDAO_MySQL(this));
        registerDAO(AggiornaMateriale.class, new AggiornaMaterialeDAO_MySQL(this));
        registerDAO(AggiornaMezzo.class, new AggiornaMezzoDAO_MySQL(this));
        registerDAO(Amministratore.class, new AmministratoreDAO_MySQL(this));
        registerDAO(InfoMissione.class, new InfoMissioneDAO_MySQL(this));
        registerDAO(Materiale.class, new MaterialeDAO_MySQL(this));
        registerDAO(Mezzo.class, new MezzoDAO_MySQL(this));
        registerDAO(Missione.class, new MissioneDAO_MySQL(this));
        registerDAO(Operatore.class, new OperatoreDAO_MySQL(this));
        registerDAO(OperatoreHaAbilita.class, new OperatoreHaAbilitaDAO_MySQL(this));
        registerDAO(OperatoreHaPatente.class, new OperatoreHaPatenteDAO_MySQL(this));
        registerDAO(Patenti.class, new PatenteDAO_MySQL(this));
        registerDAO(RichiestaSoccorso.class, new RichiestaSoccorsoDAO_MySQL(this));
    }
    
    public AbilitaDAO getAbilitaDAO() {
        return (AbilitaDAO) getDAO(Abilita.class);
    }
    
    public AggiornaMaterialeDAO getAggiornaMaterialeDAO(){
        return (AggiornaMaterialeDAO) getDAO(Materiale.class);
    }
    
    public AggiornaMezzoDAO getAggiornaMezzoDAO(){
        return (AggiornaMezzoDAO) getDAO(Mezzo.class);
    }
    
    public AmministratoreDAO getAmministratoreDAO() {
        return (AmministratoreDAO) getDAO(Amministratore.class);
    }
    
    public InfoMissioneDAO getInfoMissioneDAO(){
        return (InfoMissioneDAO) getDAO(InfoMissione.class);
    }
    
    public MaterialeDAO getMaterialeDAO(){
        return (MaterialeDAO) getDAO(Materiale.class);
    }
    
    public MezzoDAO getMezzoDAO(){
        return (MezzoDAO) getDAO(Mezzo.class);
    }
    
    public MissioneDAO getMissioneDAO(){
        return (MissioneDAO) getDAO(Missione.class);
    }
    
    public OperatoreDAO getOperatoreDAO() {
        return (OperatoreDAO) getDAO(Operatore.class);
    }
    
    public OperatoreHaAbilitaDAO getOperatoreHaAbilitaDAO(){
        return (OperatoreHaAbilitaDAO) getDAO(OperatoreHaAbilita.class);
    }
    
    public OperatoreHaPatenteDAO getOperatoreHaPatenteDAO(){
        return (OperatoreHaPatenteDAO) getDAO(OperatoreHaPatente.class);
    }
    
    public PatenteDAO getPatentiDAO(){
        return (PatenteDAO) getDAO(Patenti.class);
    }
    
    public RichiestaSoccorsoDAO getRichiestaSoccorsoDAO(){
        return (RichiestaSoccorsoDAO) getDAO(RichiestaSoccorso.class);
    }

}
