package searchengine.services;


import com.sun.istack.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.dto.indexing.IndexDto;
import searchengine.dto.indexing.LemmaDto;
import searchengine.dto.indexing.PageDto;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.RecursiveAction;

import static java.lang.Thread.sleep;

public class Crawler extends RecursiveAction {

    private String url;
    private SiteEntity site;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private Node node;

    List<PageDto> pageDtoList = new CopyOnWriteArrayList<>();

    CopyOnWriteArraySet<String> urls;


    public Crawler(Node node, String url, SiteEntity site, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.node = node;
        this.url = url;
        this.site = site;
        urls = new CopyOnWriteArraySet<>();
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        node = new Node(url);

    }

    @Override
    protected void compute() {
        try {
            sleep(1000);
            Document doc = Jsoup.connect(node.getUrl()).userAgent("Mozilla").get();
            Elements elements = doc.select("body").select("a");

            Page page = new Page();
            setPageInfo(page, site, node.getUrl().substring(0, Math.min(node.getUrl().length(), 254)), 200, doc.html());
            pageRepository.saveAndFlush(page);
            PageDto pDto = new PageDto(page.getPath(), page.getContent(), page.getCode());
            pageDtoList.add(pDto);
            LemmaIndexing lemmaIndexing = new LemmaIndexing();
            HashMap<String, Integer> lemmaMap = lemmaIndexing.getLemmas(page.getContent());
            for(Map.Entry<String, Integer> entry : lemmaMap.entrySet()) {
                Lemma l = new Lemma();
                Index i = new Index();
                l.setSiteId(site);
                l.setLemma(entry.getKey().substring(0, Math.min(entry.getKey().length(), 254)));
                l.setFrequency(1);
                lemmaRepository.saveAndFlush(l);
                LemmaDto lDto = new LemmaDto(site.getId(), entry.getKey(), l.getFrequency());
                i.setPageId(page);
                i.setLemmaId(l);
                i.setLemmaRank(entry.getValue());
                indexRepository.saveAndFlush(i);
                IndexDto iDto = new IndexDto(page.getId(), l.getId(), entry.getValue());

            for (Element e : elements) {
                String childUrl = e.absUrl("href");
                if(!childUrl.isEmpty() && !childUrl.contains("#") && !childUrl.matches("([^\\s]+(\\.(?i)(jpg|JPG|jpeg|jar|pptx|ppt|zip|png|gif|bmp|pdf))$)") &&
                        !childUrl.matches("#([\\w\\-]+)?$") && !childUrl.equals(node.getUrl()) && childUrl.contains(node.getUrl())) {
                    childUrl = childUrl.replaceAll("\\?.+", "");
                    Node newNode = new Node(childUrl);
                    node.addChild(newNode);

                }
                }
            }
        } catch (IOException | InterruptedException ex) {
            System.out.println(ex.toString());
        }
        for (Node child : node.getChildren()) {
            Crawler action = new Crawler(child, child.getUrl(), site, pageRepository, lemmaRepository, indexRepository);
            action.compute();
        }
    }

    public void setPageInfo(Page page, SiteEntity siteEntity, String path, int code, String content) {
        page.setSiteId(siteEntity);
        page.setPath(path);
        page.setCode(code);
        page.setContent(content.substring(0, Math.min(content.length(), 16777215)));

    }


        }








