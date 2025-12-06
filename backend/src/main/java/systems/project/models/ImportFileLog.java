package systems.project.models;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "import_file_log")
public class ImportFileLog {

    @Id
    @SequenceGenerator(name = "import_file_log_seq_gen",
            sequenceName = "import_file_log_seq",
            allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "import_file_log_seq_gen")
    private Long id;

    private String originalFilename;

    @Column(nullable = false)
    private String storageKey;

    private String contentType;

    private Long size;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ImportStatus status = ImportStatus.PENDING;

    @Column(length = 2048)
    private String errorMessage;

    private Integer requested;

    private Integer imported;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "import_file_log_ticket_ids",
            joinColumns = @JoinColumn(name = "log_id"))
    @Column(name = "ticket_id")
    private List<Integer> ticketIds = new ArrayList<>();

    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    @PrePersist
    public void prePersist() {
        createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }
}
