package searchengine.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.SiteEntity;
import java.util.List;
import java.util.Set;

@Repository
@Component
public interface PageRepository extends JpaRepository<Page, Integer> {
    List<Page> findAllBySiteId(SiteEntity siteId);
    Page findPageByPath(String path);
    @Query(value = "select * from Page p where p.id in :ids", nativeQuery = true)
    List<Page> findAllByIdList (@Param("ids") Set<Integer> idList);




}
