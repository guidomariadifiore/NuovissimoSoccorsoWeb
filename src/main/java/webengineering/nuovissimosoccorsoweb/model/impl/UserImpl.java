package webengineering.nuovissimosoccorsoweb.model.impl;

import webengineering.nuovissimosoccorsoweb.model.User;

import java.util.ArrayList;
import java.util.List;
import webengineering.nuovissimosoccorsoweb.model.Patenti;

public class UserImpl implements User {

    private int id;
    private String nome;
    private String cognome;
    private String email;
    private String password;
    private String cf;
    private String ruolo;
    private int idCreatore;
    private List<Patenti> patenti = new ArrayList<>();
    private List<String> abilita = new ArrayList<>();
    private int version;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getNome() {
        return nome;
    }

    @Override
    public void setNome(String nome) {
        this.nome = nome;
    }

    @Override
    public String getCognome() {
        return cognome;
    }

    @Override
    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }
    
    @Override
    public String getCf(){
        return this.cf;
    }
    
    @Override
    public void setCf(String cf){
        this.cf=cf;
    }
    
    

    @Override
    public String getRuolo() {
        return ruolo;
    }

    @Override
    public void setRuolo(String ruolo) {
        this.ruolo = ruolo;
    }
    
    @Override
    public int getIdCreatore(){
        return this.idCreatore;
    }
    
    @Override
    public void setIdCreatore(int idCreatore){
        this.idCreatore=idCreatore;
    }

    @Override
    public List<Patenti> getPatenti() {
        return patenti;
    }

    @Override
    public void setPatenti(List<Patenti> patenti) {
        this.patenti = patenti;
    }

    @Override
    public List<String> getAbilita() {
        return abilita;
    }

    @Override
    public void setAbilita(List<String> abilita) {
        this.abilita = abilita;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public void setVersion(int version) {
        this.version = version;
    }
} 
