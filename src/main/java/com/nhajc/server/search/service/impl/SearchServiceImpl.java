package com.nhajc.server.search.service.impl;
import com.nhajc.server.search.service.SearchService;
import com.nhajc.server.search.repository.DiscoveryRepository;
import com.nhajc.server.common.dto.*;
import com.nhajc.server.common.service.DiscoveryViews;
import com.nhajc.server.library.entity.*;
import com.nhajc.server.user.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
public class SearchServiceImpl implements SearchService {
    private final DiscoveryRepository discovery;
    private final DiscoveryViews views;
    private final UserRepository users;
    public SearchServiceImpl(DiscoveryRepository discovery, DiscoveryViews views, UserRepository users) {
        this.discovery=discovery; this.views=views; this.users=users;
    }
    @Override @Transactional(readOnly=true)
    public PageResponse<?> search(UUID actor, ItemType type, String q, Pageable pageable) {
        requireUser(actor);
        return views.responses(discovery.find(actor,type,q,false,false,LibrarySort.NAME,pageable),actor,type==null);
    }
    public void requireUser(UUID actor) {
        if(!users.existsById(actor))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"User no longer exists.");
    }
}

