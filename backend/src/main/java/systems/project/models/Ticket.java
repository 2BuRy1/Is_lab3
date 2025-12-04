package systems.project.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.EnumType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;


import jakarta.persistence.JoinColumn;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
public class Ticket {
    @SequenceGenerator(
            name = "ticket_seq_gen",
            sequenceName = "ticket_seq",
            allocationSize = 1
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ticket_seq_gen")
    @Id
    private Integer id; //Поле не может быть null,
    // Значение поля должно быть больше 0,
    // Значение этого поля должно быть уникальным,
    // Значение этого поля должно генерироваться автоматически

    @Column(nullable = false)
    private String name; //Поле не может быть null, Строка не может быть пустой

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "coordinates_id", nullable = false)
    private Coordinates coordinates; //Поле не может быть null

    private java.time.LocalDateTime creationDate; //Поле не может быть null,
    // Значение этого поля должно генерироваться автоматически

    @ManyToOne
    @JoinColumn(name = "person_id")
    private Person person; //Поле может быть null

    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event; //Поле может быть null

    @Check(constraints = "price > 0")
    private float price; //Значение поля должно быть больше 0

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketType type; //Поле не может быть null

    @Check(constraints = "discount > 0 and discount <= 100")
    private Float discount; //Поле может быть null,
    // Значение поля должно быть больше 0,
    // Максимальное значение поля: 100

    @Check(constraints = "number > 0")
    private int number; //Значение поля должно быть больше 0


    private String comment; //Поле может быть null

    @ManyToOne
    @JoinColumn(name = "venue_id", unique = false)
    private Venue venue; //Поле может быть null

    @PrePersist
    void prePersist() {
        creationDate = creationDate == null ?
                LocalDateTime.now() :
                    creationDate;
    }
}
