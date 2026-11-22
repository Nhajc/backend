package com.nhajc.server.release.service;

import com.nhajc.server.release.dto.ReleaseRequest;
import java.util.UUID;

public interface ReleaseService { UUID create(ReleaseRequest request); }
