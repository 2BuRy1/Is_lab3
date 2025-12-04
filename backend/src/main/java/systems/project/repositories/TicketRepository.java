package systems.project.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import systems.project.models.Ticket;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findAllBy();

    Optional<Ticket> findById(Integer id);

    boolean existsById(Integer id);

    void deleteById(Integer id);

    Long deleteByComment(String comment);

    Optional<Ticket> findFirstByEventIsNotNullOrderByEventIdAsc();

    Long countByCommentLessThan(String comment);
}
