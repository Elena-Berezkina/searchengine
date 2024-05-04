package searchengine.model;
import com.sun.istack.NotNull;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;


import javax.persistence.*;
import javax.persistence.Index;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity(name = "page")
@NoArgsConstructor(force = true)
@Table(name = "page",
        indexes = @Index(name = "path_index", columnList = "path", unique = false))
public class PageEntity {
    @Id
    @NotNull
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SiteEntity siteId;
    @NotNull
    @Column(columnDefinition = "VARCHAR(255)")
    private String path;
    @NotNull
    private int code;
    @NotNull
    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;
    @OneToMany(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "page_id")
    private List<searchengine.model.Index> indexes = new ArrayList<>();
}
