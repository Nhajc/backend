package com.nhajc.server.library.service;

import com.nhajc.server.common.dto.PageResponse;
import com.nhajc.server.library.entity.*;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface LibraryService {
    PageResponse<?> list(UUID actor, ItemType type, Pageable page);

    void save(UUID actor, ItemType type, UUID id);

    void remove(UUID actor, ItemType type, UUID id);
}
