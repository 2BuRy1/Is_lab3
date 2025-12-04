package systems.project.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import systems.project.models.Location;



@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {


}
