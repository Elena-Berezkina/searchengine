package searchengine.services;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.exceptions.*;
import searchengine.model.*;
import searchengine.repository.*;
import searchengine.task.CrawlerTask;
import searchengine.task.ParserTask;
import searchengine.utils.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.net.URL;

@Service
@EnableConfigurationProperties(value = SitesList.class)
@Slf4j
public class IndexingServiceImpl implements IndexingService{
    private CopyOnWriteArraySet<String> urlSet;
    private CopyOnWriteArrayList<SiteEntity> sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaIndexing lemmaIndexing;
    private final ModelObjectBuilder objectBuilder;
    private SitesList sitesList;

    public IndexingServiceImpl(SiteRepository siteRepository, PageRepository pageRepository,
                               LemmaRepository lemmaRepository, IndexRepository indexRepository, SitesList sitesList) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.sitesList = sitesList;
        this.urlSet = new CopyOnWriteArraySet<>();
        this.sites = new CopyOnWriteArrayList<>();
        this.lemmaIndexing = new LemmaIndexing();
        this.objectBuilder = new ModelObjectBuilder();                                                                        //заполнение всех model объектов вынесено в отдельный класс, чтобы не было повторения и чтобы код не выглядел чересчур громоздко
    }

    public void toDelete(String path) {
        if (siteRepository.findSiteByPath(path) != null) {
            if(siteRepository.findSiteByPath(path).getStatus().equals(Status.INDEXING)) {
                throw new StartIndexingException();
            }
            List<Page> pagesToDelete = pageRepository.findAllBySiteId(siteRepository.findSiteByPath(path));
            deleteLemmasAndIndexes(siteRepository.findSiteByPath(path));
            pageRepository.deleteAll(pagesToDelete);
            siteRepository.delete(siteRepository.findSiteByPath(path));
            }
    }

   public void toDeletePageData(String path) {
        if(pageRepository.findPageByPath(path) != null) {
            Page pageToDelete = pageRepository.findPageByPath(path);
            SiteEntity s = pageToDelete.getSiteId();
            deleteLemmasAndIndexes(s);
            pageRepository.delete(pageToDelete);
        }
   }

   public void deleteLemmasAndIndexes(SiteEntity site) {
       List<Lemma> lemmasToDelete = new ArrayList<>();
       List<Index> indexesToDelete = new ArrayList<>();
       if (lemmaRepository.findAllBySiteId(site).isPresent()) {
           lemmasToDelete = lemmaRepository.findAllBySiteId(site).get();
           for (Lemma lemma : lemmasToDelete) {
               if (indexRepository.findByLemmaId(lemma).isPresent()) {
                   indexesToDelete.add(indexRepository.findByLemmaId(lemma).get());
               }
           }
       }
       if(!indexesToDelete.isEmpty()) {
           indexRepository.deleteAll(indexesToDelete);
       }
       if (!lemmasToDelete.isEmpty() && indexesToDelete.isEmpty()) {
           lemmaRepository.deleteAll(lemmasToDelete);
       }
   }

    public void generalParser() {
        for (Site site : sitesList.getSites()) {
                toDelete(site.getUrl());
                ParserTask siteToParse = new ParserTask(site.getUrl());                                                                    //создается объект класса Parser, который имплементирует Callable
                FutureTask<SiteEntity> task = new FutureTask<>(siteToParse);
                Thread thread = new Thread(task);
                thread.start();
            SiteEntity newSite = null;
            try {
                newSite = siteToParse.call();                                                                                   //каждый сайт создается в новом потоке
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            siteRepository.saveAndFlush(newSite);
                urlSet.add(site.getUrl());
                new ForkJoinPool().invoke(new CrawlerTask(new Node(site.getUrl()), newSite, pageRepository,                     //при обходе страниц при переходе по каждой новой ссылке создается новый поток
                        lemmaRepository, indexRepository));
                newSite.setStatus(Status.INDEXED);
                siteRepository.saveAndFlush(newSite);
                sites.add(newSite);
        }
    }

    public void stopIndexing() {
        List<Status> statusList = new ArrayList<>();
        List<SiteEntity> sites = siteRepository.findAll();
        for(SiteEntity s : sites) {
            statusList.add(s.getStatus());
        }
        if(!statusList.contains(Status.INDEXING)) {
            throw new StopIndexingException();
        } else {
            siteRepository.findAllByStatus(Status.INDEXING)
                    .forEach(site -> objectBuilder.setStopIndexingSiteInfo(site, siteRepository));
        }
    }

    public List<String> pathList() {
        List<String> pathList = new ArrayList<>();
        sitesList.getSites().forEach(site -> pathList.add(site.getUrl()));
        return pathList;
    }

    public boolean isOffTheSiteList(String path) {
        int isOffTheListIndex = 0;
        for (String url : pathList()) {
            if (path.contains(url)) {
                isOffTheListIndex++;
            }
        }
        return isOffTheListIndex <= 0;
    }


    public void indexOnePage(String path) {
        if(!isValid(path)) { throw new WrongPathException(); }
        if(!isFound(path)) { throw new PageNotFoundException(); }
        if(isOffTheSiteList(path)) { throw new OffTheListException(); }
            toDeletePageData(path);
            Page page = new Page();
        try {
            String content = lemmaIndexing.getHtmlText(path);
            objectBuilder.setPageInfo(page, path, 200, content);
            setSiteIdForThisPage(path, page);
            pageRepository.saveAndFlush(page);
            HashMap<String, Integer> lemmaMap = lemmaIndexing.getLemmas(content);
            lemmaMap.entrySet()                                                                                          //заполняются таблицы лемм и индексов
                    .forEach(entry -> objectBuilder.createLemmaAndIndex(page.getSiteId(), page, entry.getKey(),
                            entry.getValue(), lemmaRepository, indexRepository));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setSiteIdForThisPage(String path, Page page) {
        sitesList.getSites().stream()
                .filter(site -> path.contains(site.getUrl()))
                .forEach(site -> {
                    if (siteRepository.findSiteByPath(site.getUrl()) != null) {                                          //если сайт есть в репозитории, его id записывается в site_id страницы
                        page.setSiteId(siteRepository.findSiteByPath(site.getUrl()));
                    } else if (siteRepository.findSiteByPath(site.getUrl()) == null) {                                   //если сайт не проиндексирован, он создается, и тогда уже его id записывается в site_id страницы
                        SiteEntity siteEntity = objectBuilder.setSiteEntityInfo(Status.INDEXED, LocalDateTime.now(),
                                site.getUrl(), site.getName());
                        siteRepository.saveAndFlush(siteEntity);
                        page.setSiteId(siteEntity);
                    }
                });
    }

    public boolean isValid(String path){
        try {
            URL url = new URL(path);
            url.toURI();
            return true;
        } catch(MalformedURLException | URISyntaxException ex) {
            log.info(ex.getMessage());
        }
        return false;

    }

    public boolean isFound(String path) {
        try{
            URL url = new URL(path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
        }
        catch(IOException e) {
            log.info(e.getMessage());
            return false;
        }
    }
}






























































