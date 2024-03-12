package searchengine.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.SiteEntity;
import java.util.List;
import java.util.Optional;

@Repository
@Component
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    Optional<List<Lemma>> findAllBySiteId(SiteEntity siteId);
    List<Lemma> findAllByLemma(String lemma);
    List<Lemma> findAllBySiteIdAndLemma(SiteEntity site, String lemma);
    boolean existsByLemma(String lemma);
}
