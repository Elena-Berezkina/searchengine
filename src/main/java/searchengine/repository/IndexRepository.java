package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.List;
@Repository
@Component
public interface IndexRepository extends JpaRepository<Index, Integer> {

    Index findByLemmaId(Lemma lemma);

    List<Index> findAllByPageId(Page page);



}
