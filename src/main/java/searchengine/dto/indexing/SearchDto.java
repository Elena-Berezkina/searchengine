package searchengine.dto.indexing;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SearchDto {
    String site;
    String siteName;
    String uri;
    String title;
    String snippet;
    float relevance;
}
