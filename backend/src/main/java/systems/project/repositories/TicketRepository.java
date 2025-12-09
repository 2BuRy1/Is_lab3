package systems.project.repositories;

import jakarta.persistence.QueryHint;
import org.hibernate.jpa.HibernateHints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;
import systems.project.models.Ticket;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @QueryHints(@QueryHint(name = HibernateHints.HINT_CACHEABLE, value = "true"))
    List<Ticket> findAllBy();

    Optional<Ticket> findById(Integer id);

    boolean existsById(Integer id);

    void deleteById(Integer id);

    Long deleteByComment(String comment);

    @QueryHints(@QueryHint(name = HibernateHints.HINT_CACHEABLE, value = "true"))
    Optional<Ticket> findFirstByEventIsNotNullOrderByEventIdAsc();

    @QueryHints(@QueryHint(name = HibernateHints.HINT_CACHEABLE, value = "true"))
    Long countByCommentLessThan(String comment);
}
