package com.nhajc.server.library.service;
import com.nhajc.server.common.dto.PageResponse;
import com.nhajc.server.library.entity.*;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
public interface LibraryService {
    PageResponse<?> list(UUID actor, ItemType type, String q, LibrarySort sort, boolean owned, Pageable page);
    void save(UUID actor, ItemType type, UUID id);
    void remove(UUID actor, ItemType type, UUID id);
}

