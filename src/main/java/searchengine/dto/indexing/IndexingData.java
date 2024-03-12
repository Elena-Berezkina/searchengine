package searchengine.dto.indexing;
import lombok.Data;

@Data
public class IndexingData {
    int pageId;
    int lemmaId;
    float rank;
}
