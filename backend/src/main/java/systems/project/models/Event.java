package systems.project.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Check;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.OneToMany;
import java.util.List;

@Entity
@Data
@EqualsAndHashCode
public class Event {


    @Id
    @SequenceGenerator(
            name = "event_seq_gen",
            sequenceName = "event_seq",
            allocationSize = 1
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
            generator = "event_seq_gen")
    private Integer id; //Поле не может быть null,
    // Значение поля должно быть больше 0,
    // Значение этого поля должно быть уникальным,
    // Значение этого поля должно генерироваться автоматически

    @Column(nullable = false)
    @Check(constraints = "char_length(name) > 0")
    private String name; //Поле не может быть null,
    // Строка не может быть пустой

    @Column(nullable = false)
    @Check(constraints = "tickets_count > 0")
    private Integer ticketsCount; //Поле не может быть null,
    // Значение поля должно быть больше 0

    @Enumerated(EnumType.STRING)
    private EventType eventType; //Поле может быть null

    @OneToMany
    @JsonIgnore
    private List<Ticket> tickets;
}
