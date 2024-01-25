package searchengine.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.model.SiteEntity;
import searchengine.model.Status;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.Callable;

public class Parser implements Callable<SiteEntity> {

    private String url;

    public Parser(String url) {
        this.url = url;
    }

    public SiteEntity call() throws IOException {
    Document doc = Jsoup.connect(url).userAgent("Mozilla").get();
    SiteEntity site = new SiteEntity();
    site.setName(doc.select("title").text().substring(0, Math.min(doc.select("title").text().length(), 254)));
    site.setUrl(url);
    site.setStatus(Status.INDEXING);
    site.setStatusTime(LocalDateTime.now());
    return site;

}


}
