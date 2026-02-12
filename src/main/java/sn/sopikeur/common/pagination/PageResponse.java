package sn.sopikeur.common.pagination;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PageResponse<T> {
    private List<T> items;
    private long total;
}
