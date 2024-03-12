package searchengine.services;
import java.io.IOException;

public interface IndexingService {
    void generalParser() throws IOException;
    void stopIndexing();
    void indexOnePage(String path) throws IOException;
    boolean isValid(String path);
    boolean isFound(String path);
    boolean isOffTheSiteList(String path);




}
