package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import searchengine.model.SiteEntity;

import java.util.List;

@Repository
@Component
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {
    @Query("SELECT s FROM site s WHERE s.url = :path")
    SiteEntity findSiteByPath(String path);


}
