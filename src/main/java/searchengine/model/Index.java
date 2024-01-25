package searchengine.model;

import com.sun.istack.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity(name = "lemma_index")
@Table(name = "lemma_index")
@NoArgsConstructor(force = true)
@Getter
@Setter
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @NotNull
    int id;
    @JoinColumn(name = "page_id")
    @OneToOne(fetch = FetchType.LAZY)
    Page pageId;
    @JoinColumn(name = "lemma_id")
    @OneToOne(fetch = FetchType.LAZY)
    Lemma lemmaId;
    @Column(name = "lemma_rank")
    float lemmaRank;
}
