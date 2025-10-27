package Tables;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "\"Book\"")
@JsonPropertyOrder({ "ID_Book", "ISBN", "Title", "Sinopse" })
public class Book implements Serializable {

    @Column(name = "\"ID_Book\"", nullable = false)
    private Integer ID_Book;

    @Column(name = "\"ISBN\"", nullable = false, length = Integer.MAX_VALUE)
    private String isbn;

    @Column(name = "\"Title\"", nullable = false, length = Integer.MAX_VALUE)
    private String title;

    @Column(name = "\"Sinopse\"", nullable = false, length = Integer.MAX_VALUE)
    private String sinopse;

    @JsonProperty("ID_Book")
    public List<Integer> getID_Book() {
        return Collections.singletonList(ID_Book);
    }

    public int getid(){
        return ID_Book;
    }
    @JsonProperty("ID_Book")
    public void setID_Book(Integer ID_Book) {
        this.ID_Book = ID_Book;
    }

    @JsonProperty("ISBN")
    public String getIsbn() {
        return isbn;
    }

    @JsonProperty("ISBN")
    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    @JsonProperty("Title")
    public String getTitle() {
        return title;
    }

    @JsonProperty("Title")
    public void setTitle(String title) {
        this.title = title;
    }

    @JsonProperty("Sinopse")
    public String getSinopse() {
        return sinopse;
    }

    @JsonProperty("Sinopse")
    public void setSinopse(String sinopse) {
        this.sinopse = sinopse;
    }
}
