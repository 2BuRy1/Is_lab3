package systems.project.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import systems.project.models.ImportFileLog;

import java.util.List;

@Repository
public interface ImportFileLogRepository extends JpaRepository<ImportFileLog, Long> {

    List<ImportFileLog> findAllByOrderByCreatedAtDesc();
}
