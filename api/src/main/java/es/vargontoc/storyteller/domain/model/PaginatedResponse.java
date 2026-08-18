package es.vargontoc.storyteller.domain.model;

import java.util.List;

import org.springframework.data.domain.Page;

public record PaginatedResponse<T>(
    List<T> items,
    int pageSize,
    int currentPage,
    int totalPages,
    long totalItems
) {
    public static <T> PaginatedResponse<T> from(Page<T> page) {
        return new PaginatedResponse<>(
            page.getContent(),
            page.getSize(),
            page.getNumber(),
            page.getTotalPages(),
            page.getTotalElements()
        );
    }
}
