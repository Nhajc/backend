package com.nhajc.server.admin.service;

import com.nhajc.server.artist.dto.ArtistSummaryResponse;
import com.nhajc.server.common.dto.PageResponse;
import com.nhajc.server.common.service.MediaViews;
import com.nhajc.server.common.service.UserAccess;
import com.nhajc.server.library.entity.ItemType;
import com.nhajc.server.release.dto.ReleaseSummaryResponse;
import com.nhajc.server.search.repository.DiscoveryRepository;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminCatalogService {
    private final DiscoveryRepository discovery;
    private final MediaViews views;
    private final UserAccess users;

    public AdminCatalogService(DiscoveryRepository discovery, MediaViews views, UserAccess users) {
        this.discovery = discovery;
        this.views = views;
        this.users = users;
    }

    public PageResponse<ArtistSummaryResponse> artists(UUID actor, String query, Pageable page) {
        return list(actor, ItemType.ARTIST, query, page, ids -> views.artists(ids, actor));
    }

    public PageResponse<ReleaseSummaryResponse> releases(UUID actor, String query, Pageable page) {
        return list(actor, ItemType.RELEASE, query, page, ids -> views.releases(ids, actor));
    }

    private <T> PageResponse<T> list(
            UUID actor, ItemType type, String query, Pageable page, Function<Collection<UUID>, Map<UUID, T>> load) {
        users.require(actor);
        var hits = discovery.browse(actor, type, query, page);
        var ids = hits.content().stream().map(DiscoveryRepository.Hit::id).toList();
        var values = load.apply(ids);
        return hits.map(hit -> values.get(hit.id()));
    }
}
