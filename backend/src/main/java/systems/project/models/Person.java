package systems.project.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.JoinColumn;

import lombok.Data;
import org.hibernate.annotations.Check;

import java.util.List;
@Data
@Entity
public class Person {
    @SequenceGenerator(
            name = "person_seq_gen",
            sequenceName = "person_seq",
            allocationSize = 1
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
            generator = "person_seq_gen")
    @Id
    private Long id;

    @Column(name = "eye_color")
    @Enumerated(EnumType.STRING)
    private Color eyeColor; //Поле может быть null

    @Column(name = "hair_color")
    @Enumerated(EnumType.STRING)
    private Color hairColor; //Поле не может быть null

    @OneToOne
    @JoinColumn(name = "location_id")
    private Location location; //Поле не может быть null

    @Column(nullable = false)
    @Check(constraints = "weight > 0")
    private Double weight; //Поле не может быть null,
    // Значение поля должно быть больше 0

    @Column(nullable = false)
    private String passportID; //Поле не может быть null

    @Enumerated(EnumType.STRING)
    private Country nationality; //Поле не может быть null

    @OneToMany(mappedBy = "person")
    @JsonIgnore
    private List<Ticket> tickets;
}
