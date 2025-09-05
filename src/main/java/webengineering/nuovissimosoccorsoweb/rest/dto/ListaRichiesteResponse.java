package webengineering.nuovissimosoccorsoweb.rest.dto;

import java.util.List;

/**
 * DTO per la risposta della lista richieste paginata. 
 */
public class ListaRichiesteResponse {

    private List<RichiestaDTO> content;
    private int totalElements;
    private int totalPages;
    private int number; // Numero pagina corrente 
    private int size;   // Elementi per pagina
    private boolean first;
    private boolean last;

    // Costruttori
    public ListaRichiesteResponse() {
    }

    public ListaRichiesteResponse(List<RichiestaDTO> content, int totalElements,
            int totalPages, int number, int size,
            boolean first, boolean last) {
        this.content = content;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.number = number;
        this.size = size;
        this.first = first;
        this.last = last;
    }

    // Getters e Setters
    public List<RichiestaDTO> getContent() {
        return content;
    }

    public void setContent(List<RichiestaDTO> content) {
        this.content = content;
    }

    public int getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(int totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public boolean isFirst() {
        return first;
    }

    public void setFirst(boolean first) {
        this.first = first;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }

    @Override
    public String toString() {
        return "ListaRichiesteResponse{"
                + "content=" + (content != null ? content.size() : 0) + " items"
                + ", totalElements=" + totalElements
                + ", totalPages=" + totalPages
                + ", number=" + number
                + ", size=" + size
                + ", first=" + first
                + ", last=" + last
                + '}';
    }
}
