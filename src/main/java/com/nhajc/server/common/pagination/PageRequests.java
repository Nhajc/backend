package com.nhajc.server.common.pagination;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class PageRequests {
    public static final String DEFAULT_PAGE = "0";
    public static final String DEFAULT_SIZE = "20";
    public static final String DEFAULT_COMMENT_SIZE = "10";
    public static final int MAX_SIZE = 100;

    private PageRequests() {}

    public static Pageable of(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page must be at least 0 and size must be between 1 and 100.");
        }
        if ((long) page * size > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page offset exceeds the supported range.");
        }
        return PageRequest.of(page, size);
    }
}
