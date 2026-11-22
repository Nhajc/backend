package com.nhajc.server.release.service.impl;
import com.nhajc.server.release.service.ReleaseReadService;
import com.nhajc.server.release.dto.ReleaseResponse;
import com.nhajc.server.release.repository.ReleaseRepository;
import com.nhajc.server.song.repository.SongRepository;
import com.nhajc.server.library.entity.*;
import com.nhajc.server.library.repository.SavedReleaseRepository;
import com.nhajc.server.common.dto.PageResponse;
import com.nhajc.server.common.service.*;
import com.nhajc.server.common.pagination.PageRequests;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;

@Service
public class ReleaseReadServiceImpl implements ReleaseReadService {
    private final ReleaseRepository releases; private final SongRepository songs;
    private final UserAccess users; private final MediaViews views; private final SavedReleaseRepository saved;
    public ReleaseReadServiceImpl(ReleaseRepository releases,SongRepository songs,UserAccess users,MediaViews views,SavedReleaseRepository saved) {
        this.releases=releases;this.songs=songs;this.users=users;this.views=views;this.saved=saved;
    }
    @Override @Transactional
    public ReleaseResponse get(UUID actor,UUID id,Pageable pageable) {
        var page=PageRequests.of(pageable.getPageNumber(),pageable.getPageSize());
        users.lock(actor);
        if(!releases.existsById(id))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Release not found.");
        saved.findById(new LibraryId(actor,id)).ifPresent(LibraryEntry::touch);
        var slice=songs.findByRelease_IdOrderByTrackNumberAscIdAsc(id,page);
        var mapped=views.songs(slice.getContent().stream().map(song->song.getId()).toList(),actor);
        return new ReleaseResponse(views.releases(List.of(id),actor).get(id),PageResponse.from(slice,song->mapped.get(song.getId()).summary()));
    }
}

