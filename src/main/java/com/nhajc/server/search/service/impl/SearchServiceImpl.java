package com.nhajc.server.search.service.impl;
import com.nhajc.server.search.service.SearchService;
import com.nhajc.server.search.repository.DiscoveryRepository;
import com.nhajc.server.common.dto.*;
import com.nhajc.server.common.service.DiscoveryViews;
import com.nhajc.server.library.entity.*;
import com.nhajc.server.common.service.UserAccess;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class SearchServiceImpl implements SearchService {
    private final DiscoveryRepository discovery;
    private final DiscoveryViews views;
    private final UserAccess users;
    public SearchServiceImpl(DiscoveryRepository discovery, DiscoveryViews views, UserAccess users) {
        this.discovery=discovery; this.views=views; this.users=users;
    }
    @Override @Transactional(readOnly=true)
    public PageResponse<?> search(UUID actor, ItemType type, String q, Pageable pageable) {
        users.require(actor);
        return views.responses(discovery.find(actor,type,q,false,false,LibrarySort.NAME,pageable),actor,type==null);
    }
}

