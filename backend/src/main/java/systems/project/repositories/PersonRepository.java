package systems.project.repositories;

import jakarta.persistence.QueryHint;
import org.hibernate.jpa.HibernateHints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;
import systems.project.models.Person;

import java.util.List;
@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {
    @QueryHints(@QueryHint(name = HibernateHints.HINT_CACHEABLE, value = "true"))
    List<Person> findAllBy();

    boolean existsPersonByPassportID(String passportID);

}
