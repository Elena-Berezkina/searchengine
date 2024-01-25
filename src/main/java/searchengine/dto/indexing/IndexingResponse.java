package searchengine.dto.indexing;

import lombok.Data;
import org.springframework.http.HttpStatus;

import java.util.List;

@Data
public class IndexingResponse {
    private boolean result;
    private int count;
    private String error;
    private HttpStatus status;
    private List<SearchDto> data;
    public IndexingResponse (boolean result) {
        this.result = result;
    }
    public IndexingResponse(boolean result, String error) {
        this.result = result;
        this.error = error;
    }

    public IndexingResponse(boolean result, int count, List<SearchDto> data, HttpStatus status) {
        this.result = result;
        this.count = count;
        this.data = data;
        this.status = status;
    }
}
