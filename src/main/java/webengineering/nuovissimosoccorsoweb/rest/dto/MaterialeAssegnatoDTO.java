package webengineering.nuovissimosoccorsoweb.rest.dto;

/**
 * DTO per un materiale assegnato a una missione.
 */
public class MaterialeAssegnatoDTO {
    private int id;
    private String nome;
    private int quantita;
    
    // Costruttori
    public MaterialeAssegnatoDTO() {}
    
    public MaterialeAssegnatoDTO(int id, String nome, int quantita) {
        this.id = id;
        this.nome = nome;
        this.quantita = quantita;
    }
    
    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    
    public int getQuantita() { return quantita; }
    public void setQuantita(int quantita) { this.quantita = quantita; }
    
    @Override
    public String toString() {
        return "MaterialeAssegnatoDTO{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", quantita=" + quantita +
                '}';
    }
}