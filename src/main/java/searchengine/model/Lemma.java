package searchengine.model;

import com.sun.istack.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity(name = "lemma")
@NoArgsConstructor(force = true)
@Getter
@Setter
@Table(name = "lemma")
public class Lemma {
    @Id
    @NotNull
    @GeneratedValue(strategy = GenerationType.AUTO)
    int id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    SiteEntity siteId;
    @NotNull
    @Column(columnDefinition = "VARCHAR(255)")
    String lemma;
    @NotNull
    int frequency;
}
