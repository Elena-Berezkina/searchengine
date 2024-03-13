package searchengine.task;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.dto.indexing.PageDto;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.utils.LemmaIndexing;
import searchengine.utils.ModelObjectBuilder;
import searchengine.utils.Node;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RecursiveAction;
import static java.lang.Thread.sleep;

public class CrawlerTask extends RecursiveAction {
    private SiteEntity site;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final Node node;
    private final LemmaIndexing lemmaIndexing;
    private final List<PageDto> pageDtoList;
    private final ModelObjectBuilder objectBuilder;

    public CrawlerTask(Node node, SiteEntity site, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.node = node;
        this.site = site;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        lemmaIndexing = new LemmaIndexing();
        pageDtoList = new CopyOnWriteArrayList<>();
        objectBuilder = new ModelObjectBuilder();
    }

    @Override
    protected void compute() {
        try {
            sleep(1000);
            Document doc = Jsoup.connect(node.getUrl()).userAgent("Mozilla").get();
            Elements elements = doc.select("body").select("a");
            Page page = new Page();
            pageDtoList.add(createPageObject(page, doc));
            HashMap<String, Integer> lemmaMap = lemmaIndexing.getLemmas(page.getContent());
            lemmaMap.entrySet().forEach(entry -> objectBuilder.createLemmaAndIndex(site, page, entry.getKey()
                            .substring(0, Math.min(entry.getKey().length(), 254)),
                    entry.getValue(), lemmaRepository, indexRepository));
            for (Element e : elements) {
                String childUrl = e.absUrl("href");
                if (!childUrl.isEmpty() && isCorrectUrl(childUrl) && !childUrl.equals(node.getUrl()) &&
                        childUrl.contains(node.getUrl())) {
                    childUrl = childUrl.replaceAll("\\?.+", "");
                    Node newNode = new Node(childUrl);
                    node.addChild(newNode);
                }
            }
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
        }
        for (Node child : node.getChildren()) {
            CrawlerTask action = new CrawlerTask(child, site, pageRepository, lemmaRepository, indexRepository);
            action.compute();
        }
    }

    public PageDto createPageObject(Page page, Document doc) {
        objectBuilder.setPageInfo(page, node.getUrl().substring(0, Math.min(node.getUrl().length(), 254)), 200,
                doc.html());
        page.setSiteId(site);
        pageRepository.saveAndFlush(page);
        return new PageDto(page.getPath(), page.getContent(), page.getCode());

    }

    public boolean isCorrectUrl(String url) {
        return (!url.contains("#")
                && !url.matches("([^\\s]+(\\.(?i)(jpg|JPG|jpeg|jar|pptx|ppt|zip|png|gif|bmp|pdf))$)") &&
                !url.matches("#([\\w\\-]+)?$"));
    }
}













