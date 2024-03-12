package searchengine.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;
import searchengine.model.Lemma;
import java.util.Optional;

@Repository
@Component
public interface IndexRepository extends JpaRepository<Index, Integer> {
    Optional<Index> findByLemmaId(Lemma lemma);
}
