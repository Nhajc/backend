package com.nhajc.server.common.service;
import com.nhajc.server.search.repository.DiscoveryRepository;
import com.nhajc.server.common.dto.*;
import com.nhajc.server.library.entity.ItemType;
import org.springframework.stereotype.Component;
import java.util.*;
@Component
public class DiscoveryViews {
    private final MediaViews views;
    public DiscoveryViews(MediaViews views) { this.views=views; }
    public PageResponse<?> responses(PageResponse<DiscoveryRepository.Hit> page, UUID actor, boolean mixed) {
        var ids=new EnumMap<ItemType,List<UUID>>(ItemType.class);
        for(var type:ItemType.values())ids.put(type,new ArrayList<>());
        page.content().forEach(hit->ids.get(hit.type()).add(hit.id()));
        var values=new EnumMap<ItemType,Map<UUID,?>>(ItemType.class);
        values.put(ItemType.SONG,views.songs(ids.get(ItemType.SONG),actor));
        values.put(ItemType.RELEASE,views.releases(ids.get(ItemType.RELEASE),actor));
        values.put(ItemType.PLAYLIST,views.playlists(ids.get(ItemType.PLAYLIST),actor));
        values.put(ItemType.ARTIST,views.artists(ids.get(ItemType.ARTIST),actor));
        var result=new ArrayList<Object>();
        for(var hit:page.content()) {
            Object item=values.get(hit.type()).get(hit.id());
            if(item!=null)result.add(mixed ? new TypedItemResponse<>(hit.type().name(),item) : item);
        }
        return new PageResponse<>(result,page.page(),page.size(),page.hasNext());
    }
}

