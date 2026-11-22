package com.nhajc.server.release.service;
import com.nhajc.server.release.dto.ReleaseResponse;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
public interface ReleaseReadService { ReleaseResponse get(UUID actor,UUID id,Pageable page); }

