package searchengine.services;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteEntity;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;


@Service

@EnableConfigurationProperties(value = SitesList.class)
public class StatisticsServiceImpl implements StatisticsService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    public StatisticsServiceImpl(SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
    }

    public DetailedStatisticsItem getDetailedStatData(SiteEntity site) {
        DetailedStatisticsItem detStatData = new DetailedStatisticsItem();
        detStatData.setUrl(site.getUrl());
        detStatData.setName(site.getName());
        detStatData.setStatus(site.getStatus().toString());
        detStatData.setStatusTime(site.getStatusTime().atZone(ZoneId.of("Europe/Moscow")).toEpochSecond());
        detStatData.setError(site.getLastError());
        detStatData.setPages(pageRepository.findAllBySiteId(site).size());
        if(lemmaRepository.findAllBySiteId(site).isPresent()) {
            detStatData.setLemmas(lemmaRepository.findAllBySiteId(site).get().size());
        }
        return detStatData;
    }

    public List<DetailedStatisticsItem> getDetailedStatList() {
        List<DetailedStatisticsItem> list = new ArrayList<>();
        for (SiteEntity s : siteRepository.findAll()) {
            list.add(getDetailedStatData(s));
        }
        return list;
    }

    public TotalStatistics getTotalStatData() {
        TotalStatistics statData = new TotalStatistics();
        statData.setSites(siteRepository.findAll().size());
        statData.setPages(pageRepository.findAll().size());
        statData.setLemmas(lemmaRepository.findAll().size());
        statData.setIndexing(true);
        return statData;
    }

    @Override
    public StatisticsResponse getStatistics() {
        StatisticsData data = new StatisticsData(getTotalStatData(), getDetailedStatList());
        return new StatisticsResponse(true, data);
    }
}








