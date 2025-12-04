package systems.project.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import systems.project.models.Person;

import java.util.List;
@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {
    List<Person> findAllBy();

    boolean existsPersonByPassportID(String passportID);

}
