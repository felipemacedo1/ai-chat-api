package com.chatai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResponse<T> {
    
    private List<T> data;
    private PaginationMeta meta;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginationMeta {
        private long total;
        private int page;
        private int limit;
        private int totalPages;
    }
    
    public static <E, T> PaginatedResponse<T> fromPage(Page<E> page, Function<E, T> mapper) {
        List<T> data = page.getContent().stream()
                .map(mapper)
                .collect(Collectors.toList());
        
        PaginationMeta meta = PaginationMeta.builder()
                .total(page.getTotalElements())
                .page(page.getNumber())
                .limit(page.getSize())
                .totalPages(page.getTotalPages())
                .build();
        
        return PaginatedResponse.<T>builder()
                .data(data)
                .meta(meta)
                .build();
    }
}
