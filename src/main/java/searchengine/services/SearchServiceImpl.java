package searchengine.services;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.indexing.SearchDto;
import searchengine.exceptions.EmptyQueryException;
import searchengine.model.*;
import searchengine.repository.*;
import searchengine.utils.LemmaIndexing;
import searchengine.utils.ModelObjectBuilder;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


@Service
@EnableConfigurationProperties(value = SitesList.class)
@Slf4j
public class SearchServiceImpl implements SearchService {
    private final LemmaIndexing lemmaIndexing;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final ModelObjectBuilder objectBuilder;

    public SearchServiceImpl(SiteRepository siteRepository, PageRepository pageRepository,
                             LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.lemmaIndexing = new LemmaIndexing();
        this.objectBuilder = new ModelObjectBuilder();
    }

    public List<Lemma> getSortedLemmaList(String text) {
        Set<Lemma> lemmaList = new HashSet<>();
        HashMap<String, Integer> lemmaAndFrequencyMap = lemmaIndexing.getLemmas(text);
        lemmaAndFrequencyMap.forEach((key, value) -> {
            Lemma lemma = objectBuilder.createLemma(key, lemmaRepository);
            if (lemma.getFrequency() <= 20) {
                lemmaList.add(lemma);
            }
        });
      return lemmaList.stream()
              .sorted(compareByFrequency())
              .toList();
    }

    public List<Lemma> getLemmaListForAllSitesSearch(String text) {
        String lemma = getSortedLemmaList(text).get(0).getLemma();
        return lemmaRepository.findAllByLemma(lemma);
    }

    public List<Lemma> getLemmaListForOneSiteSearch(String text, String path) {
        SiteEntity site = siteRepository.findSiteByPath(path);
        String lemma = getSortedLemmaList(text).get(0).getLemma();
        return lemmaRepository.findAllBySiteIdAndLemma(site, lemma);
    }

    public List<PageEntity> getPageList(String text, List<Lemma> lemmaList) {
        List<PageEntity> firstLemmaPageList = pageListForLemmas(lemmaList);
        List<PageEntity> firstLemmaPageListFiltered = new ArrayList<>(firstLemmaPageList);
        List<Lemma> sortedLemmaList = getSortedLemmaList(text);
        for(PageEntity page : firstLemmaPageList) {
            for(Lemma lemma : sortedLemmaList.subList(1, sortedLemmaList.size())) {
                lemma = objectBuilder.getLemmaFromRepository(lemmaRepository, lemma, lemma.getLemma(), page.getSiteId());
                if(!lemmaIndexing.pageContainsLemma(page, lemma)) {
                    firstLemmaPageListFiltered.remove(page);
                }
            }
        }
        return firstLemmaPageListFiltered;
    }

    public List<PageEntity> pageListForLemmas(List<Lemma> lemmaList) {
        List<PageEntity> lemmaPageList = new ArrayList<>();
        for(Lemma lemma : lemmaList) {
            if (lemmaRepository.existsById(lemma.getId())) {
                Optional<Index> indexOptional = indexRepository.findByLemmaId(lemma);
                indexOptional.ifPresent(index -> lemmaPageList.add(index.getPageId()));
            }
        }
        return lemmaPageList;
    }

    public double getAbsoluteRelevance(PageEntity page) {
        List<Float> rankList = page.getIndexes().stream().map(Index::getLemmaRank).toList();
        double[] rankListToArray = rankList.stream()
                .mapToDouble(i -> i)
                .toArray();
        return Arrays.stream(rankListToArray)
                .reduce(0, Double::sum);
    }

    public HashMap<Integer, Float> getRelevanceMap(String text, List<Lemma> lemmaList) {
        List<Double> relList = new ArrayList<>();
        HashMap<Integer, Float> pageMap = new HashMap<>();
        List<PageEntity> pageList = getPageList(text, lemmaList);
        if (pageList != null) {
            for (PageEntity page : pageList) {
                double  absRel = getAbsoluteRelevance(page);
                relList.add(absRel);
                pageMap.put(page.getId(), (float) absRel);
            }
            pageMap.entrySet()
                    .forEach(entry -> entry.setValue((float) (entry.getValue() / Collections.max(relList))));
            return pageMap;
        }
        return null;
    }

    public List<SearchDto> getPageDataList(String text, List<Lemma> lemmaList) {
        List<SearchDto> pageObjList = new ArrayList<>();
        HashMap<Integer, Float> relevanceMap = getRelevanceMap(text, lemmaList);
        Set<Integer> idList = relevanceMap.keySet();
        List<PageEntity> pageList = pageRepository.findAllByIdList(idList);
        for(PageEntity page : pageList) {
            SiteEntity siteEntity = page.getSiteId();
            String site = siteEntity.getUrl();
            String siteName = siteEntity.getName();
            String uri = page.getPath();
            try {
                String title = setTitle(page);
                String snippet = lemmaIndexing.setResultSnippet(page, text);
                float relevance = relevanceMap.get(page.getId());
                SearchDto pageObj = new SearchDto(site, siteName, uri, title, snippet, relevance);
                pageObjList.add(pageObj);
            } catch (IOException e) {
                log.info(e.getMessage());
            }
        }
        return pageObjList
                .stream()
                .sorted(Comparator.comparing(SearchDto::getRelevance)
                .reversed())
                .collect(Collectors.toList());

    }

    @Override
    public List<SearchDto> startSearch(String query, String path)  {
        if (query.isEmpty()) {
            throw new EmptyQueryException();
        } else if (query != null && path != null) {
            return getPageDataList(query, getLemmaListForOneSiteSearch(query, path));
        } else if (query != null && path == null)
            return getPageDataList(query, getLemmaListForAllSitesSearch(query));
        return null;
    }

    public Page<SearchDto> searchWithPagination(List<SearchDto> results, Integer offset, Integer limit)  {
        Pageable pageable = PageRequest.of(offset, limit);
        int start = Math.min((int) pageable.getOffset(), offset + limit);
        int end = Math.min((start + pageable.getPageSize()), results.size());
        return new PageImpl<>(results.subList(start, end), pageable, results.size());
    }

    public String setTitle(PageEntity page) {
        String text = page.getContent();
        String titleStartTeg = "<title>";
        String titleEndTeg  ="</title>";
        return text.substring(text.indexOf(titleStartTeg) + titleStartTeg.length(), text.indexOf(titleEndTeg));
    }

    public Comparator <Lemma> compareByFrequency() {
        return Comparator.comparing(Lemma::getFrequency);
    }
}






















