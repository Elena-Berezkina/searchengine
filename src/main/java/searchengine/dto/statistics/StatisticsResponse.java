package searchengine.dto.statistics;
import lombok.AllArgsConstructor;
import lombok.Data;
@AllArgsConstructor
@Data
public class StatisticsResponse {
    private boolean result;
    private StatisticsData statistics;
}
