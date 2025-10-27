package Tables;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "\"Author\"")
public class Author {
    @Id
    @Column(name = "\"ID_Author\"", nullable = false)
    private Integer id;

    @Column(name = "\"Name\"", nullable = false, length = Integer.MAX_VALUE)
    private String name;

    @Column(name = "\"Age\"", nullable = false)
    private Integer age;

    @Column(name = "\"Biography\"", nullable = false, length = Integer.MAX_VALUE)
    private String biography;

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

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

}