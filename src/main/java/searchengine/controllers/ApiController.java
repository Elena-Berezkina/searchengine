package searchengine.controllers;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.*;
import java.io.IOException;



@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;


    public ApiController(StatisticsService statisticsService, IndexingServiceImpl indexingService,
                         SearchService searchService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.searchService = searchService;

    }

    @RequestMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @RequestMapping("/startIndexing")
    public IndexingResponse startIndexing() throws IOException {
        indexingService.generalParser();
        return new IndexingResponse(true);
    }

    @GetMapping("/stopIndexing")
    public IndexingResponse stopIndexing() {
        indexingService.stopIndexing();
        return new IndexingResponse(true);
    }

    @PostMapping("/indexPage")
    public IndexingResponse indexOnePage(@RequestParam(name = "path") String path) throws IOException {
            indexingService.indexOnePage(path);
        return new IndexingResponse(true);
    }

    @GetMapping("/search")
    public IndexingResponse search(@RequestParam(name = "query") String query,
                                            @RequestParam(name = "site", required = false) String site) throws IOException {

        return new IndexingResponse(true, searchService.startSearch(query, site).size(),
                searchService.startSearch(query, site), HttpStatus.OK);
    }







}













