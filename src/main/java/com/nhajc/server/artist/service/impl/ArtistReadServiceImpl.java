package com.nhajc.server.artist.service.impl;
import com.nhajc.server.artist.service.ArtistReadService;
import com.nhajc.server.artist.dto.ArtistResponse;
import com.nhajc.server.artist.repository.ArtistRepository;
import com.nhajc.server.release.repository.ReleaseRepository;
import com.nhajc.server.common.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
@Service
public class ArtistReadServiceImpl implements ArtistReadService {
    private final ArtistRepository artists; private final ReleaseRepository releases;
    private final UserAccess users; private final MediaViews views;
    public ArtistReadServiceImpl(ArtistRepository artists,ReleaseRepository releases,UserAccess users,MediaViews views) {
        this.artists=artists;this.releases=releases;this.users=users;this.views=views;
    }
    @Override @Transactional(readOnly=true)
    public ArtistResponse get(UUID actor,UUID id) {
        users.require(actor);
        var artist=artists.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Artist not found."));
        var values=releases.findForArtist(id);
        var mapped=views.releases(values.stream().map(release->release.getId()).toList(),actor);
        return new ArtistResponse(views.artist(artist,actor),values.stream().map(release->mapped.get(release.getId())).toList());
    }
}

