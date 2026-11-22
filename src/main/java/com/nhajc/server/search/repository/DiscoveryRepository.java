package com.nhajc.server.search.repository;

import com.nhajc.server.library.entity.*;
import com.nhajc.server.common.dto.PageResponse;
import com.nhajc.server.common.pagination.PageRequests;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

/**
 * Entity queries through JPA. Hibernate HQL union merges heterogeneous IDs before
 * applying offset/limit in the database; no native SQL or in-memory global paging.
 */
@Repository
public class DiscoveryRepository {
    public record Hit(UUID id, ItemType type) {}
    private final EntityManager entities;
    public DiscoveryRepository(EntityManager entities) { this.entities=entities; }

    public PageResponse<Hit> find(UUID actor, ItemType type, String input, boolean library,
                                  boolean owned, LibrarySort sort, Pageable pageable) {
        var page=PageRequests.of(pageable.getPageNumber(),pageable.getPageSize());
        String q=input==null ? null : input.strip().toLowerCase(Locale.ROOT);
        if((!library&&q==null) || q!=null&&(q.isEmpty()||q.length()>100))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"q must contain 1–100 characters.");
        if(type==ItemType.ARTIST&&sort==LibrarySort.ARTIST)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"ARTIST sort is not supported for artists.");
        List<ItemType> types=type!=null ? List.of(type) : library ? List.of(ItemType.RELEASE,ItemType.PLAYLIST) : List.of(ItemType.values());
        var branches=new ArrayList<String>();
        for(var kind:types) branches.add(branch(kind,library,q!=null,owned));
        String order=!library ? "matchRank, itemName, itemType, itemId"
                : sort==LibrarySort.RECENT ? "activity desc, itemName, itemType, itemId"
                : sort==LibrarySort.ARTIST ? "artistName, itemName, itemType, itemId" : "itemName, itemType, itemId";
        var query=entities.createQuery(String.join(" union all ",branches)+" order by "+order,Object[].class);
        if(library||types.contains(ItemType.PLAYLIST))query.setParameter("actor",actor);
        if(q!=null) {
            query.setParameter("exact",q);
            query.setParameter("contains","%"+escape(q)+"%");
            query.setParameter("prefix",escape(q)+"%");
        }
        var rows=query.setFirstResult(Math.toIntExact(page.getOffset())).setMaxResults(page.getPageSize()+1).getResultList();
        boolean next=rows.size()>page.getPageSize();
        var hits=rows.stream().limit(page.getPageSize()).map(row->new Hit((UUID)row[0],ItemType.valueOf((String)row[1]))).toList();
        return PageResponse.of(hits,page,next);
    }
    private static String escape(String value) { return value.replace("!","!!").replace("%","!%").replace("_","!_"); }
    private static String branch(ItemType type, boolean library, boolean filtered, boolean owned) {
        String entity=switch(type){case SONG->"Song";case RELEASE->"Release";case PLAYLIST->"Playlist";case ARTIST->"Artist";};
        String label=type==ItemType.ARTIST ? "e.name" : "e.title";
        String saved=switch(type){case SONG->"FavoriteSong";case RELEASE->"SavedRelease";case PLAYLIST->"SavedPlaylist";case ARTIST->"FollowedArtist";};
        String firstArtist=switch(type) {
            case SONG,RELEASE -> "coalesce((select min(lower(a.name)) from "+entity+" x join x.artists a where x.id=e.id),'')";
            case PLAYLIST -> "lower(e.owner.username)";
            case ARTIST -> "lower(e.name)";
        };
        String rank=filtered ? "case when lower("+label+")=:exact then 0 when lower("+label+") like :prefix escape '!' then 1 else 2 end" : "0";
        String activity=library ? "s.activityAt" : "cast(null as Instant)";
        String from=library ? " from "+saved+" s, "+entity+" e where s.id.itemId=e.id and s.id.userId=:actor" : " from "+entity+" e where 1=1";
        String filter=filtered ? " and lower("+label+") like :contains escape '!'" : "";
        if(type==ItemType.PLAYLIST) {
            filter+=" and (e.isPrivate=false or e.owner.id=:actor)";
            if(owned)filter+=" and e.owner.id=:actor";
        }
        return "select e.id as itemId, '"+type.name()+"' as itemType, lower("+label+") as itemName, "
                +firstArtist+" as artistName, "+rank+" as matchRank, "+activity+" as activity"+from+filter;
    }
}

