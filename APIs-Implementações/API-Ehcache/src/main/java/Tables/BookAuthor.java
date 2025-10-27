package Tables;

import jakarta.persistence.*;

import java.util.Objects;


@Table(name = "\"Book_Author\"")
public class BookAuthor {

    @MapsId("idBook")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "\"ID_Book\"", nullable = false)
    private Book idBook;

    @MapsId("idAuthor")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "\"ID_Author\"", nullable = false)
    private Author idAuthor;

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


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BookAuthor that)) return false;
        return Objects.equals(idBook, that.idBook) && Objects.equals(idAuthor, that.idAuthor);
    }

}