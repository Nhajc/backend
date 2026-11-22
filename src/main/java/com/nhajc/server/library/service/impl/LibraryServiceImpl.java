package com.nhajc.server.library.service.impl;
import com.nhajc.server.library.service.LibraryService;
import com.nhajc.server.library.entity.*;
import com.nhajc.server.library.repository.*;
import com.nhajc.server.artist.repository.ArtistRepository;
import com.nhajc.server.release.repository.ReleaseRepository;
import com.nhajc.server.song.repository.SongRepository;
import com.nhajc.server.playlist.repository.PlaylistRepository;
import com.nhajc.server.common.dto.PageResponse;
import com.nhajc.server.common.service.*;
import com.nhajc.server.search.repository.DiscoveryRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@Service
public class LibraryServiceImpl implements LibraryService {
    private final UserAccess users;
    private final ArtistRepository artists;
    private final ReleaseRepository releases;
    private final SongRepository songs;
    private final PlaylistRepository playlists;
    private final SavedReleaseRepository savedReleases;
    private final SavedPlaylistRepository savedPlaylists;
    private final FollowedArtistRepository follows;
    private final FavoriteSongRepository favorites;
    private final DiscoveryRepository discovery;
    private final DiscoveryViews views;
    public LibraryServiceImpl(UserAccess users, ArtistRepository artists, ReleaseRepository releases,
            SongRepository songs, PlaylistRepository playlists, SavedReleaseRepository savedReleases,
            SavedPlaylistRepository savedPlaylists, FollowedArtistRepository follows, FavoriteSongRepository favorites,
            DiscoveryRepository discovery, DiscoveryViews views) {
        this.users=users;this.artists=artists;this.releases=releases;this.songs=songs;this.playlists=playlists;
        this.savedReleases=savedReleases;this.savedPlaylists=savedPlaylists;this.follows=follows;this.favorites=favorites;
        this.discovery=discovery;this.views=views;
    }
    @Override @Transactional(readOnly=true)
    public PageResponse<?> list(UUID actor, ItemType type, String q, LibrarySort sort, boolean owned, Pageable page) {
        users.require(actor);
        if(owned&&type!=ItemType.PLAYLIST)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"owned is only supported for playlists.");
        return views.responses(discovery.find(actor,type,q,true,owned,sort,page),actor,type==null);
    }
    @Override @Transactional
    public void save(UUID actor, ItemType type, UUID id) {
        users.lock(actor);
        boolean exists=switch(type) {
            case RELEASE -> releases.existsById(id);
            case SONG -> songs.existsById(id);
            case ARTIST -> artists.existsById(id);
            case PLAYLIST -> {
                var p=playlists.findLockedById(id).orElseThrow(LibraryServiceImpl::missing);
                if(p.isPrivate()&&!p.getOwner().getId().equals(actor))throw missing();
                yield true;
            }
        };
        if(!exists)throw missing();
        LibraryId key=new LibraryId(actor,id);
        switch(type) {
            case RELEASE -> { if(!savedReleases.existsById(key))savedReleases.save(new SavedRelease(key)); }
            case PLAYLIST -> { if(!savedPlaylists.existsById(key))savedPlaylists.save(new SavedPlaylist(key)); }
            case ARTIST -> { if(!follows.existsById(key))follows.save(new FollowedArtist(key)); }
            case SONG -> { if(!favorites.existsById(key))favorites.save(new FavoriteSong(key)); }
        }
    }
    @Override @Transactional
    public void remove(UUID actor, ItemType type, UUID id) {
        users.lock(actor);
        if(type==ItemType.PLAYLIST) {
            var p=playlists.findLockedById(id);
            if(p.isPresent()) {
                if(p.get().getOwner().getId().equals(actor))
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Delete your own playlist using DELETE /playlists/{id}.");
                if(p.get().isPrivate())throw missing();
            }
        }
        LibraryId key=new LibraryId(actor,id);
        switch(type) {
            case RELEASE -> savedReleases.deleteById(key);
            case PLAYLIST -> savedPlaylists.deleteById(key);
            case ARTIST -> follows.deleteById(key);
            case SONG -> favorites.deleteById(key);
        }
    }
    private static ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND,"Resource not found."); }
}

