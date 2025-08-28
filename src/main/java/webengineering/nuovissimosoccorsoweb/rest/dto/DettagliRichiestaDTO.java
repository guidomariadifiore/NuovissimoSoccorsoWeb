// ===== DettagliRichiestaDTO.java =====
package webengineering.nuovissimosoccorsoweb.rest.dto;

/**
 * DTO per i dettagli completi di una richiesta di soccorso. Include
 * informazioni sulla missione associata (se esistente).
 */
public class DettagliRichiestaDTO {

    private int id;
    private String stato;
    private String descrizione;
    private String indirizzo;
    private String nome;
    private String coordinate;
    private String foto;
    private String ip;
    private String emailSegnalante;
    private String nomeSegnalante;
    private Integer idAmministratore;
    private String nomeAmministratore;
    private MissioneAssociataDTO missioneAssociata;

    // Costruttori
    public DettagliRichiestaDTO() {
    }

    // Getters e Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStato() {
        return stato;
    }

    public void setStato(String stato) {
        this.stato = stato;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getIndirizzo() {
        return indirizzo;
    }

    public void setIndirizzo(String indirizzo) {
        this.indirizzo = indirizzo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(String coordinate) {
        this.coordinate = coordinate;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getEmailSegnalante() {
        return emailSegnalante;
    }

    public void setEmailSegnalante(String emailSegnalante) {
        this.emailSegnalante = emailSegnalante;
    }

    public String getNomeSegnalante() {
        return nomeSegnalante;
    }

    public void setNomeSegnalante(String nomeSegnalante) {
        this.nomeSegnalante = nomeSegnalante;
    }

    public Integer getIdAmministratore() {
        return idAmministratore;
    }

    public void setIdAmministratore(Integer idAmministratore) {
        this.idAmministratore = idAmministratore;
    }

    public String getNomeAmministratore() {
        return nomeAmministratore;
    }

    public void setNomeAmministratore(String nomeAmministratore) {
        this.nomeAmministratore = nomeAmministratore;
    }

    public MissioneAssociataDTO getMissioneAssociata() {
        return missioneAssociata;
    }

    public void setMissioneAssociata(MissioneAssociataDTO missioneAssociata) {
        this.missioneAssociata = missioneAssociata;
    }
}
