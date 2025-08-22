package webengineering.nuovissimosoccorsoweb.rest.dto;

/**
 * DTO per un mezzo assegnato a una missione.
 */
public class MezzoAssegnatoDTO {
    private String targa;
    private String tipo;
    private String modello;
    
    // Costruttori
    public MezzoAssegnatoDTO() {}
    
    public MezzoAssegnatoDTO(String targa, String tipo, String modello) {
        this.targa = targa;
        this.tipo = tipo;
        this.modello = modello;
    }
    
    // Getters e Setters
    public String getTarga() { return targa; }
    public void setTarga(String targa) { this.targa = targa; }
    
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    
    public String getModello() { return modello; }
    public void setModello(String modello) { this.modello = modello; }
    
    @Override
    public String toString() {
        return "MezzoAssegnatoDTO{" +
                "targa='" + targa + '\'' +
                ", tipo='" + tipo + '\'' +
                ", modello='" + modello + '\'' +
                '}';
    }
}