package searchengine.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.SiteEntity;

import java.util.List;

@Repository
@Component
public interface PageRepository extends JpaRepository<Page, Integer> {

List<Page> findAllBySiteId(SiteEntity siteId);

Page findPageByPath(String path);




}
