package Tables;

import jakarta.persistence.*;

import java.io.Serializable;

@Entity
@Table(name = "\"Reservations\"")
public class Reservation implements Serializable {
    @Id
    @Column(name = "\"ID_Reservation\"", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "\"ID_Library\"", nullable = false)
    private int idLibrary;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "\"ID_Book\"", nullable = false)
    private int idBook;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "\"ID_Clients\"", nullable = false)
    private int idClients;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getIdLibrary() {
        return idLibrary;
    }

    public void setIdLibrary(int idLibrary) {
        this.idLibrary = idLibrary;
    }

    public int getIdBook() {
        return idBook;
    }

    public void setIdBook(int idBook) {
        this.idBook = idBook;
    }

    public int getIdClients() {
        return idClients;
    }

    public void setIdClients(int idClients) {
        this.idClients = idClients;
    }

}