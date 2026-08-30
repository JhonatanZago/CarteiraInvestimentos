package com.example.carteirainvestimento.dto.common;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T, R> PageResponse<R> from(Page<T> source, Function<T, R> mapper) {
        return new PageResponse<>(source.map(mapper).getContent(), source.getNumber(), source.getSize(),
                source.getTotalElements(), source.getTotalPages());
    }
}
