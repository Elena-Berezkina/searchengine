package searchengine.model;

import com.sun.istack.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Entity(name = "site")
@Getter
@Setter
@NoArgsConstructor(force = true)
@Table(name = "site")
public class SiteEntity {
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Id
    @NotNull
    private int id;
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')")
    private Status status;
    @NotNull
    @Column(name = "status_time", columnDefinition = "DATETIME")
    private LocalDateTime statusTime;
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
    @NotNull
    @Column(columnDefinition = "VARCHAR(255)")
    private String url;
    @NotNull
    @Column(columnDefinition = "VARCHAR(255)")
    private String name;
    @OneToMany(mappedBy = "siteId", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Page> pages;
    @OneToMany(mappedBy = "siteId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Lemma> lemmas;

}
