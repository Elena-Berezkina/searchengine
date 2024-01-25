package searchengine.dto.indexing;

import lombok.Data;

import java.util.List;

@Data
public class IndexingData {
    int pageId;
    int lemmaId;
    float rank;
}
