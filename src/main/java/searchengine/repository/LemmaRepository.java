package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.SiteEntity;

import java.util.List;
@Repository
@Component
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

boolean existsByLemma (String lemma);

List<Lemma> findAllBySiteId(SiteEntity siteId);

List<Lemma> findAllByLemma(String lemma);
}
