package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingData;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.PageDto;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.exceptions.*;
import searchengine.model.*;
import searchengine.repository.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import java.net.URL;

import static java.lang.Thread.sleep;

@Service
@EnableConfigurationProperties(value = SitesList.class)
public class IndexingServiceImpl implements IndexingService{
    CopyOnWriteArraySet<String> urlSet = new CopyOnWriteArraySet<>();
    CopyOnWriteArrayList<SiteEntity> sites = new CopyOnWriteArrayList<>();
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    @Autowired
    SitesList sitesList;

    public IndexingServiceImpl(SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    public void toDelete(String path) {
        if (siteRepository.findSiteByPath(path) != null) {
            if(siteRepository.findSiteByPath(path).getStatus().equals(Status.INDEXING)) {
                throw new StartIndexingException();
            }
            List<Page> pagesToDelete = pageRepository.findAllBySiteId(siteRepository.findSiteByPath(path));
            List<Lemma> lemmasToDelete = lemmaRepository.findAllBySiteId(siteRepository.findSiteByPath(path));
            List<Index> indexesToDelete = new ArrayList<>();
            for (Lemma l : lemmasToDelete) {
                if (indexRepository.findByLemmaId(l) != null) {
                    indexesToDelete.add(indexRepository.findByLemmaId(l));
                }
                if (!indexesToDelete.isEmpty()) {
                    indexRepository.deleteAll(indexesToDelete);
                }
                if (!lemmasToDelete.isEmpty() && indexesToDelete.isEmpty()) {
                    lemmaRepository.deleteAll(lemmasToDelete);
                }
                if(!pagesToDelete.isEmpty() && lemmasToDelete.isEmpty()) {
                    pageRepository.deleteAll(pagesToDelete);
                }
                siteRepository.delete(siteRepository.findSiteByPath(path));
            }
        }
    }

   public void toDeletePageData(String path) {
   if(pageRepository.findPageByPath(path) != null) {
       Page pageToDelete = pageRepository.findPageByPath(path);
       SiteEntity s = pageToDelete.getSiteId();
       List<Lemma> lemmasToDelete = lemmaRepository.findAllBySiteId(s);
       List<Index> indexesToDelete = new ArrayList<>();
       for (Lemma l : lemmasToDelete) {
           Index i = indexRepository.findByLemmaId(l);
           indexesToDelete.add(i);
       }
       if (!indexesToDelete.isEmpty()) {
           indexRepository.deleteAll(indexesToDelete);
       }
       if (!lemmasToDelete.isEmpty()) {
           lemmaRepository.deleteAll(lemmasToDelete);
       }
       pageRepository.delete(pageToDelete);
   }

   }

    public void generalParser() throws IOException {
        for (Site s : sitesList.getSites()) {
                toDelete(s.getUrl());
                Parser site = new Parser(s.getUrl());
                FutureTask<SiteEntity> task = new FutureTask<>(site);
                Thread thread = new Thread(task);
                thread.start();
                SiteEntity newSite = site.call();
                siteRepository.saveAndFlush(newSite);
                urlSet.add(s.getUrl());
                new ForkJoinPool().invoke(new Crawler(new Node(s.getUrl()), s.getUrl(), newSite, pageRepository, lemmaRepository, indexRepository));
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
            for (SiteEntity site : siteRepository.findAll()) {
                if (site.getStatus().equals(Status.INDEXING)) {
                    Thread.currentThread().interrupt();
                    site.setStatus(Status.FAILED);
                    site.setLastError("Индексация остановлена пользователем");
                    siteRepository.save(site);
                }
            }
        }
    }

    public List<String> pathList() {
        List<String> pathList = new ArrayList<>();
        for (Site site : sitesList.getSites()) {
            pathList.add(site.getUrl());
        }
        return pathList;
    }

    public boolean isOffTheSiteList(String path) {
        int i = 0;
        for (String url : pathList()) {
            if (path.contains(url)) {
                i++;
            }
        }
        return i <= 0;
    }


    public void indexOnePage(String path) throws IOException {
        if(!isValid(path)) { throw new WrongPathException(); }
        if(!isFound(path)) { throw new PageNotFoundException(); }
        if(isOffTheSiteList(path)) { throw new OffTheListException(); }
            toDeletePageData(path);
            LemmaIndexing lemmaIndexing = new LemmaIndexing();
            Page page = new Page();
            page.setPath(path);
            page.setContent(lemmaIndexing.getHtmlText(path));
            setSiteIdForThisPage(path, page);
            pageRepository.saveAndFlush(page);
            HashMap<String, Integer> lemmaMap = lemmaIndexing.getLemmas(lemmaIndexing.getHtmlText(path));
            for (Map.Entry<String, Integer> entry : lemmaMap.entrySet()) {
                Lemma lemma = new Lemma();
                Index index = new Index();
                lemma.setLemma(entry.getKey());
                setFrequency(lemma, entry.getKey());
                lemma.setSiteId(page.getSiteId());
                lemmaRepository.saveAndFlush(lemma);
                index.setPageId(page);
                index.setLemmaId(lemma);
                index.setLemmaRank(entry.getValue());
                indexRepository.saveAndFlush(index);
            }
        }


    public void setFrequency(Lemma lemma, String l) {
        int i = 1;
        if (!lemmaRepository.existsByLemma(l)) {
            lemma.setFrequency(i);
        } else if (lemmaRepository.existsByLemma(l)) {
            lemma.setFrequency(i++);
        }
    }


    public void setSiteIdForThisPage(String path, Page page) {
            for (Site s : sitesList.getSites()) {
                if (path.contains(s.getUrl())) {
                    if (siteRepository.findSiteByPath(s.getUrl()) != null) {
                        page.setSiteId(siteRepository.findSiteByPath(s.getUrl()));
                    } else if (siteRepository.findSiteByPath(s.getUrl()) == null) {
                        SiteEntity site = new SiteEntity();
                        site.setUrl(s.getUrl());
                        site.setName(s.getName());
                        site.setStatusTime(LocalDateTime.now());
                        site.setStatus(Status.INDEXED);
                        siteRepository.saveAndFlush(site);
                        page.setSiteId(site);
                    }
                }
            }
    }

    public boolean isValid(String path){
        try{
            URL url = new URL(path);
            url.toURI();
            return true;
        }
        catch(Exception e) {
            return false;
        }

    }

    public boolean isFound(String path) {
        try{
            URL url = new URL(path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
        }
        catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }






}






























































