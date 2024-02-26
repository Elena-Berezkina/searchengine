package searchengine.utils;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;

public class ModelObjectBuilder {

    public SiteEntity setSiteEntityInfo(Status status, LocalDateTime statusTime, String url, String name) {
        SiteEntity siteEntity = new SiteEntity();
        siteEntity.setStatus(status);
        siteEntity.setStatusTime(statusTime);
        siteEntity.setUrl(url);
        siteEntity.setName(name);
        return siteEntity;
    }

    public void setStopIndexingSiteInfo(SiteEntity site, SiteRepository siteRepository) {
        site.setStatus(Status.FAILED);
        site.setLastError("Индексация остановлена пользователем");
        siteRepository.save(site);
    }

    public void setPageInfo(Page page, String path, int code, String content) {
        page.setPath(path);
        page.setCode(code);
        page.setContent(content.substring(0, Math.min(content.length(), 16777215)));

    }

    public void createLemmaAndIndex(SiteEntity site, Page page, String lemmaText, int rank,
                                    LemmaRepository lRepository, IndexRepository iRepository) {

        Lemma lemma = createLemma(lemmaText, lRepository);
        saveNewLemma(lemma, site, lRepository);
        Index index = new Index();
        index.setPageId(page);
        index.setLemmaId(lemma);
        index.setLemmaRank(rank);
        iRepository.saveAndFlush(index);

    }

    public Lemma createLemma(String lemmaText, LemmaRepository lemmaRepository) {
       Lemma lemma = new Lemma();
       lemma.setLemma(lemmaText);
       setFrequency(lemma, lemmaText, lemmaRepository);
       return lemma;
    }

    public void saveNewLemma(Lemma lemma, SiteEntity site, LemmaRepository lemmaRepository) {
        lemma.setSiteId(site);
        lemmaRepository.saveAndFlush(lemma);
    }

    public Lemma getLemmaFromRepository(LemmaRepository lemmaRepository, Lemma lemma, String lemmaText, SiteEntity site) {
        if(lemmaRepository.findById(lemma.getId()).isEmpty()) {
            lemma = createLemma(lemmaText, lemmaRepository);
            saveNewLemma(lemma, site, lemmaRepository);
            return lemma;
        } else if(lemmaRepository.findById(lemma.getId()).isPresent()) {
            return lemmaRepository.findById(lemma.getId()).get();
        }
        return null;
    }

    public void setFrequency(Lemma lemma, String lemmaText, LemmaRepository lemmaRepository) {
        int frequency = 1;
        if (lemmaRepository.findAllByLemma(lemmaText).isEmpty()) {
            lemma.setFrequency(frequency);
        } else if (!lemmaRepository.findAllByLemma(lemmaText).isEmpty()) {
            lemma.setFrequency(frequency + 1);
        }
    }





}
