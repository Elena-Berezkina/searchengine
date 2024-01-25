package searchengine.services;

import searchengine.config.Site;
import searchengine.dto.indexing.SearchDto;
import searchengine.model.Page;

import java.io.IOException;
import java.util.List;

public interface SearchService {

    List<SearchDto> startAllSitesSearch(String query) throws IOException;
    List<SearchDto> startOneSiteSearch(String query, String path) throws IOException;

}
