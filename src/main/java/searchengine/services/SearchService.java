package searchengine.services;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import searchengine.dto.indexing.SearchDto;
import java.io.IOException;
import java.util.List;

public interface SearchService {
   List<SearchDto> startSearch(String query, String path) throws IOException;
   Page<SearchDto> searchWithPagination(List<SearchDto> results, Integer offset, Integer limit);
}
