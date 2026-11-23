package com.nhajc.server.search.service;
import com.nhajc.server.common.dto.PageResponse;
import com.nhajc.server.library.entity.ItemType;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
public interface SearchService { PageResponse<?> search(UUID actor, ItemType type, String q, Pageable pageable); }

