package searchengine.task;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.utils.ModelObjectBuilder;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.Callable;

public class ParserTask implements Callable<SiteEntity> {
    private String url;
    private ModelObjectBuilder builder;

    public ParserTask(String url) {
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
