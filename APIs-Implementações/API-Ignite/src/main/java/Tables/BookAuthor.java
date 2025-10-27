package Tables;

import jakarta.persistence.*;

import java.io.Serializable;

@Entity
@Table(name = "\"Book_Author\"")
public class BookAuthor implements Serializable {
    @EmbeddedId
    private BookAuthorId id;

    @MapsId("idBook")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "\"ID_Book\"", nullable = false)
    private Book idBook;

    @MapsId("idAuthor")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "\"ID_Author\"", nullable = false)
    private Author idAuthor;

    public BookAuthorId getId() {
        return id;
    }

    public void setId(BookAuthorId id) {
        this.id = id;
    }

    public Book getIdBook() {
        return idBook;
    }

    public void setIdBook(Book idBook) {
        this.idBook = idBook;
    }

    public Author getIdAuthor() {
        return idAuthor;
    }

    public void setIdAuthor(Author idAuthor) {
        this.idAuthor = idAuthor;
    }

}