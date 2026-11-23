package com.nhajc.server.genre.service;

import com.nhajc.server.genre.dto.GenreRequest;
import com.nhajc.server.genre.dto.GenreResponse;
import com.nhajc.server.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface GenreService {
    UUID create(GenreRequest request);
    PageResponse<GenreResponse> list(Pageable pageable);
}
