package Tables;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class StockId implements Serializable {
    private static final long serialVersionUID = -7862574369584490869L;
    @Column(name = "\"ID_Library\"", nullable = false)
    private Integer idLibrary;

    @Column(name = "\"ID_Book\"", nullable = false)
    private Integer idBook;

    public Integer getIdLibrary() {
        return idLibrary;
    }

    public void setIdLibrary(Integer idLibrary) {
        this.idLibrary = idLibrary;
    }

    public Integer getIdBook() {
        return idBook;
    }

    public void setIdBook(Integer idBook) {
        this.idBook = idBook;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        StockId entity = (StockId) o;
        return Objects.equals(this.idLibrary, entity.idLibrary) &&
                Objects.equals(this.idBook, entity.idBook);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idLibrary, idBook);
    }

}