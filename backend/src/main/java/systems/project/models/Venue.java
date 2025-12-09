package systems.project.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import lombok.Data;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Check;

import java.util.List;

@Entity
@Data
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Venue {

    @Id
    @SequenceGenerator(
            name = "venue_seq_gen",
            sequenceName = "venue_seq",
            allocationSize = 1
    )

    @GeneratedValue(strategy = GenerationType.SEQUENCE,
            generator = "venue_seq_gen")
    private Long id; //Поле не может быть null, Значение поля должно быть больше 0,
    // Значение этого поля должно быть уникальным,
    // Значение этого поля должно генерироваться автоматически

    @Column(nullable = false)
    @Check(constraints = "char_length(name) > 0")
    private String name; //Поле не может быть null,
    // Строка не может быть пустой

    @Check(constraints = "capacity > 0")
    private int capacity; //Значение поля должно быть больше 0

    @Enumerated(EnumType.STRING)
    private VenueType type; //Поле может быть null

    @OneToMany(mappedBy = "venue")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnore
    private List<Ticket> tickets;
}
