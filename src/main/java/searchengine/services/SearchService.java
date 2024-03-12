package searchengine.services;
import searchengine.dto.indexing.SearchDto;
import java.io.IOException;
import java.util.List;

public interface SearchService {
    List<SearchDto> startSearch(String query, String path) throws IOException;

}
