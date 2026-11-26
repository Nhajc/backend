package com.nhajc.server.genre.service;

import com.nhajc.server.common.dto.PageResponse;
import com.nhajc.server.genre.dto.GenreRequest;
import com.nhajc.server.genre.dto.GenreResponse;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface GenreService {
    UUID create(GenreRequest request);

    PageResponse<GenreResponse> list(Pageable pageable);
}
