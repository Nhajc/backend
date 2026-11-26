package com.nhajc.server.search.service;

import com.nhajc.server.common.dto.PageResponse;
import com.nhajc.server.library.entity.ItemType;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface SearchService {
    PageResponse<?> search(UUID actor, ItemType type, String q, Pageable pageable);
}
