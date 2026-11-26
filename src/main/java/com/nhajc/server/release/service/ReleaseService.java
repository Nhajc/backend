package com.nhajc.server.release.service;

import com.nhajc.server.release.dto.ReleaseRequest;
import com.nhajc.server.release.dto.ReleaseResponse;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface ReleaseService {
    UUID create(ReleaseRequest request, MultipartFile file);

    ReleaseResponse get(UUID actor, UUID id);
}
