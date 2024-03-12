package searchengine.utils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.Callable;

public class Parser implements Callable<SiteEntity> {
    private String url;
    private ModelObjectBuilder builder;

    public Parser(String url) {
        this.url = url;
        builder = new ModelObjectBuilder();
    }

    public SiteEntity call() throws IOException {
        Document doc = Jsoup.connect(url).userAgent("Mozilla").get();
        String name = doc.select("title").text().substring(0, Math.min(doc.select("title")
                .text().length(), 254));
        return builder.setSiteEntityInfo(Status.INDEXING, LocalDateTime.now(), url, name);
    }
}
