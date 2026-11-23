package com.nhajc.server.common.dto;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(List<T> content, int page, int size, boolean hasNext) {
    public PageResponse { content = List.copyOf(content); }
    public static <T> PageResponse<T> of(List<T> content, Pageable pageable, boolean hasNext) {
        return new PageResponse<>(content, pageable.getPageNumber(), pageable.getPageSize(), hasNext);
    }
    public static <E,T> PageResponse<T> from(Slice<E> slice, Function<E,T> mapper) {
        return of(slice.getContent().stream().map(mapper).toList(), slice.getPageable(), slice.hasNext());
    }
    public <R> PageResponse<R> map(Function<T,R> mapper) {
        return new PageResponse<>(content.stream().map(mapper).toList(), page, size, hasNext);
    }
}

