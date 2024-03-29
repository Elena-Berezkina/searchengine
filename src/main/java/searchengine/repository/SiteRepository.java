package searchengine.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteEntity;
import searchengine.model.Status;

import java.util.List;

@Repository
@Component
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {
    @Query("SELECT s FROM site s WHERE s.url = :path")
    SiteEntity findSiteByPath(String path);
    List<SiteEntity> findAllByStatus(Status status);


}
