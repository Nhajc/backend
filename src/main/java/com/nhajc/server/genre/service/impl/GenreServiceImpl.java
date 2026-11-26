package com.nhajc.server.genre.service.impl;

import com.nhajc.server.common.dto.PageResponse;
import com.nhajc.server.common.exception.DatabaseConflicts;
import com.nhajc.server.genre.dto.GenreRequest;
import com.nhajc.server.genre.dto.GenreResponse;
import com.nhajc.server.genre.entity.Genre;
import com.nhajc.server.genre.repository.GenreRepository;
import com.nhajc.server.genre.service.GenreService;
import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GenreServiceImpl implements GenreService {
    private final GenreRepository genres;

    public GenreServiceImpl(GenreRepository genres) {
        this.genres = genres;
    }

    @Override
    @Transactional
    public UUID create(GenreRequest request) {
        UUID id = UUID.randomUUID();
        String base =
                Normalizer.normalize(request.name(), Normalizer.Form.NFD)
                        .replaceAll("\\p{M}+", "")
                        .replace('\u0111', 'd')
                        .replace('\u0110', 'D')
                        .toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9]+", "-")
                        .replaceAll("^-|-$", "");
        if (base.isEmpty()) base = "genre";
        try {
            genres.saveAndFlush(new Genre(id, request.name(), base));
        } catch (DataIntegrityViolationException exception) {
            throw DatabaseConflicts.translate(exception, "Genre already exists.");
        }
        return id;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<GenreResponse> list(Pageable pageable) {
        var slice = genres.findPage(pageable);
        return PageResponse.from(slice, GenreResponse::from);
    }
}
