package searchengine.services;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.indexing.SearchDto;
import searchengine.exceptions.EmptyQueryException;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SiteEntity;
import searchengine.repository.*;
import searchengine.utils.LemmaIndexing;
import searchengine.utils.ModelObjectBuilder;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


@Service
@EnableConfigurationProperties(value = SitesList.class)
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
        lemmaIndexing = new LemmaIndexing();
        objectBuilder = new ModelObjectBuilder();
    }

    public List<Lemma> getSortedLemmaList(String text) {                                                                 //формирует из слов запроса список лемм
        Set<Lemma> lemmaList = new HashSet<>();
        HashMap<String, Integer> lemmaAndFrequencyMap = lemmaIndexing.getLemmas(text);
        lemmaAndFrequencyMap.forEach((key, value) -> {
            Lemma lemma = objectBuilder.createLemma(key, lemmaRepository);
            if (lemma.getFrequency() <= 20) {
                lemmaList.add(lemma);
            }
        });
      return lemmaList.stream()
              .sorted(compareByFrequency())                                                                              //список возвращается отсортированным по частоте
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

    public List<Page> getPageList(String text, List<Lemma> lemmaList) {                                                  //получение списка страниц, отвечающих на запрос
        List<Page> firstLemmaPageList = pageListForLemmas(lemmaList);
        List<Page> firstLemmaPageListFiltered = new ArrayList<>(firstLemmaPageList);
        List<Lemma> sortedLemmaList = getSortedLemmaList(text);
        for(Page page : firstLemmaPageList) {
            for(Lemma lemma : sortedLemmaList.subList(1, sortedLemmaList.size())) {
                lemma = objectBuilder.getLemmaFromRepository(lemmaRepository, lemma, lemma.getLemma(), page.getSiteId());
                if(!lemmaIndexing.pageContainsLemma(page, lemma)) {
                    firstLemmaPageListFiltered.remove(page);
                }
            }
        }
        return firstLemmaPageListFiltered;
    }

    public List<Page> pageListForLemmas(List<Lemma> lemmaList) {
        List<Page> lemmaPageList = new ArrayList<>();
        for(Lemma lemma : lemmaList) {
            if (lemmaRepository.existsById(lemma.getId())) {
                Optional<Index> indexOptional = indexRepository.findByLemmaId(lemma);
                indexOptional.ifPresent(index -> lemmaPageList.add(index.getPageId()));
            }
        }
        return lemmaPageList;
    }

    public double getAbsoluteRelevance(Page page) {
        List<Float> rankList = page.getIndexes().stream().map(Index::getLemmaRank).toList();
        double[] rankListToArray = rankList.stream()
                .mapToDouble(i -> i)
                .toArray();
        return Arrays.stream(rankListToArray)
                .reduce(0, Double::sum);                                                                          //сумма rank; перевела в double, так как при работе со стримами с double легче, чем с float
    }

    public HashMap<Integer, Float> getRelevanceMap(String text, List<Lemma> lemmaList) {                                    //получение карты релевантности, где ключ - страница, значение - относительная релевантность
        List<Double> relList = new ArrayList<>();                                                                        //список абсолютных релевантностей нужен для определения максимальной абсолютной рел-сти поисковой выдачи
        HashMap<Integer, Float> pageMap = new HashMap<>();
        List<Page> pageList = getPageList(text, lemmaList);
        if (pageList != null) {
            for (Page page : pageList) {
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

    public List<SearchDto> getPageDataList(String text, List<Lemma> lemmaList) {                      //представляет поисковую выдачу как список объектов с заданными тех заданием полями; список сортируется по убыванию релевантности
        List<SearchDto> pageObjList = new ArrayList<>();
        HashMap<Integer, Float> relevanceMap = getRelevanceMap(text, lemmaList);
        Set<Integer> idList = relevanceMap.keySet();
        List<Page> pageList = pageRepository.findAllByIdList(idList);
        for(Page page : pageList) {
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
                e.printStackTrace();
            }
        }
        return pageObjList.stream()
                .sorted(Comparator.comparing(SearchDto::getRelevance)
                .reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<SearchDto> startSearch(String query, String path)  {                                                     //если параметр путь отсутствует, поиск осущ. по всем сайтам
        if (query.isEmpty()) {
            throw new EmptyQueryException();
        } else if (query != null && path != null) {
            return getPageDataList(query, getLemmaListForOneSiteSearch(query, path));
        } else if (query != null && path == null)
            return getPageDataList(query, getLemmaListForAllSitesSearch(query));
        return null;
    }


    public String setTitle(Page page) {
        String text = page.getContent();
        String titleStartTeg = "<title>";
        String titleEndTeg  ="</title>";
        return text.substring(text.indexOf(titleStartTeg) + titleStartTeg.length(), text.indexOf(titleEndTeg));
    }

    public Comparator <Lemma> compareByFrequency() {
        return Comparator.comparing(Lemma::getFrequency);
    }
}






















