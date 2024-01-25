package searchengine.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.PageDto;
import searchengine.dto.indexing.SearchDto;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SiteEntity;
import searchengine.repository.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


@Service
@EnableConfigurationProperties(value = SitesList.class)
public class SearchServiceImpl implements SearchService {

    LemmaIndexing lemmaIndexing = new LemmaIndexing();
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    IndexingServiceImpl indexingService;




    public SearchServiceImpl(SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        indexingService= new IndexingServiceImpl(siteRepository, pageRepository, lemmaRepository, indexRepository);

    }

    public List<Lemma> getSortedLemmaList(String text) throws IOException {
        List<Lemma> lemmaList = new ArrayList<>();
        HashMap<String, Integer> lemmaMap = lemmaIndexing.getLemmas(text);
            for (Map.Entry<String, Integer> entry : lemmaMap.entrySet()) {
                        Lemma l = new Lemma();
                        l.setLemma(entry.getKey());
                        indexingService.setFrequency(l, entry.getKey());
                        if (l.getFrequency() < 20) {
                            lemmaList.add(l);
                        }
                    }
            Comparator<Lemma> compareByFrequency = Comparator.comparing(Lemma::getFrequency);
      return lemmaList.stream().sorted(compareByFrequency).toList();
    }


    public List<Lemma> getLemmaListForAllSitesSearch(String text) throws IOException {
        return lemmaRepository.findAllByLemma(getSortedLemmaList(text).get(0).getLemma());
    }

    public List<Lemma> getLemmaListForOneSiteSearch(String text, String path) throws IOException {
        List<Lemma> lemmaList = new ArrayList<>();
        SiteEntity site = siteRepository.findSiteByPath(path);
        for(Lemma lemma : getLemmaListForAllSitesSearch(text)) {
            if(lemma.getSiteId().equals(site)) {
                lemmaList.add(lemma);
            }
        }
        return lemmaList;
    }


    public List<Page> getPageList(String text, List<Lemma> lemmaList) throws IOException {
        List<Page> firstLemmaPageList = new ArrayList<>();
        List<Index> indexList = new ArrayList<>();
        //List<Lemma> lemmaList = lemmaRepository.findAllByLemma(getSortedLemmaList(text).get(0).getLemma());
        for(Lemma lemma : lemmaList) {
            Index i = indexRepository.findByLemmaId(lemma);
            indexList.add(i);
            }

        for(Index index : indexList) {
            firstLemmaPageList.add(index.getPageId());
        }

        for(Page p : firstLemmaPageList) {
            for(Lemma lemma : getSortedLemmaList(text)) {
                 if(lemmaRepository.existsByLemma(lemma.getLemma())) {
                     firstLemmaPageList.remove(p);
                     }
            }
        }
        return firstLemmaPageList;

    }

    public boolean pageContainsLemma(Page page, Lemma lemma) throws IOException {
       lemma = lemmaRepository.findById(lemma.getId()).get();
           Index i = indexRepository.findByLemmaId(lemma);
           if (i.getPageId().equals(page)) {
               return true;
           }

       return false;

    }

    public float getAbsoluteRelevance(Page page) {
        float absRel = 0;
        for (Index i : indexRepository.findAllByPageId(page)) {
                absRel = absRel + i.getLemmaRank();
                }
        return absRel;
    }


    public HashMap<Page, Float> getRelevanceMap(String text, List<Lemma> lemmaList) throws IOException {
        List<Float> relList = new ArrayList<>();
        HashMap<Page, Float> pageMap = new HashMap<>();
        if (getPageList(text, lemmaList) != null) {
            for (Page p : getPageList(text, lemmaList)) {
                float absRel = getAbsoluteRelevance(p);
                relList.add(absRel);
                pageMap.put(p, absRel);
            }

            for (Map.Entry<Page, Float> entry : pageMap.entrySet()) {
                entry.setValue(entry.getValue() / Collections.max(relList));
            }
            return pageMap;
        }
        return null;
    }

    public List<SearchDto> getPageDataList(String text, List<Lemma> lemmaList) throws IOException {
        List<SearchDto> pageObjList = new ArrayList<>();

            for (Map.Entry<Page, Float> entry : getRelevanceMap(text, lemmaList).entrySet()) {
                Page p = pageRepository.findById(entry.getKey().getId()).get();
                String site = p.getSiteId().getUrl();
                String siteName = p.getSiteId().getName();
                String uri = p.getPath();
                String title = setTitle(p.getPath());
                String snippet = lemmaIndexing.getSnippet(p.getPath(), text);
                float relevance = entry.getValue();
                SearchDto pageObj = new SearchDto(site, siteName, uri, title, snippet, relevance);
                pageObjList.add(pageObj);
            }
            return pageObjList.stream().sorted(Comparator.comparing(SearchDto::getRelevance).reversed()).collect(Collectors.toList());


    }


    @Override
    public List<SearchDto> startAllSitesSearch(String query) throws IOException {
        return getPageDataList(query, getLemmaListForAllSitesSearch(query));
    }

    public List<SearchDto> startOneSiteSearch(String query, String path) throws IOException {
        return getPageDataList(query, getLemmaListForOneSiteSearch(query, path));
    }


    public String setTitle(String path) throws IOException {
        Document doc = Jsoup.connect(path).userAgent("Mozilla").get();
        return doc.select("title").text();

    }





}






















