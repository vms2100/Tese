package Tables;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "\"Clients\"")
public class Client {
    @Id
    @Column(name = "\"ID_Clients\"", nullable = false)
    private Integer id;

    @Column(name = "\"Name\"", nullable = false, length = Integer.MAX_VALUE)
    private String name;

    @Column(name = "\"Phone\"", nullable = false, length = Integer.MAX_VALUE)
    private String phone;

    @Column(name = "\"Email\"", nullable = false, length = Integer.MAX_VALUE)
    private String email;

    @Column(name = "\"Adress\"", nullable = false, length = Integer.MAX_VALUE)
    private String adress;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAdress() {
        return adress;
    }

    public void setAdress(String adress) {
        this.adress = adress;
    }

}