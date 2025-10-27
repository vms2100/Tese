package Tables;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class BookAuthorId implements Serializable {
    private static final long serialVersionUID = 3966302389836784771L;
    @Column(name = "\"ID_Book\"", nullable = false)
    private Integer idBook;

    @Column(name = "\"ID_Author\"", nullable = false)
    private Integer idAuthor;

    public Integer getIdBook() {
        return idBook;
    }

    public void setIdBook(Integer idBook) {
        this.idBook = idBook;
    }

    public Integer getIdAuthor() {
        return idAuthor;
    }

    public void setIdAuthor(Integer idAuthor) {
        this.idAuthor = idAuthor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        BookAuthorId entity = (BookAuthorId) o;
        return Objects.equals(this.idBook, entity.idBook) &&
                Objects.equals(this.idAuthor, entity.idAuthor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idBook, idAuthor);
    }

}