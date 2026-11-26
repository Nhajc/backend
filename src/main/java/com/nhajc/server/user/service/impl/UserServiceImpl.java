package com.nhajc.server.user.service.impl;

import com.nhajc.server.common.security.Actors;
import com.nhajc.server.common.service.UserAccess;
import com.nhajc.server.storage.ImageStorage;
import com.nhajc.server.storage.ObjectStorageCleanup;
import com.nhajc.server.user.dto.UpdateProfileRequest;
import com.nhajc.server.user.dto.UserResponse;
import com.nhajc.server.user.mapper.UserResponseMapper;
import com.nhajc.server.user.repository.UserRepository;
import com.nhajc.server.user.service.UserService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository users;
    private final UserAccess access;
    private final ImageStorage images;
    private final ObjectStorageCleanup cleanup;
    private final UserResponseMapper responses;
    private final TransactionTemplate transaction;

    public UserServiceImpl(
            UserRepository users,
            UserAccess access,
            ImageStorage images,
            ObjectStorageCleanup cleanup,
            UserResponseMapper responses,
            PlatformTransactionManager manager) {
        this.users = users;
        this.access = access;
        this.images = images;
        this.cleanup = cleanup;
        this.responses = responses;
        this.transaction = new TransactionTemplate(manager);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse me(String subject) {
        return responses.from(access.require(Actors.id(subject)));
    }

    @Override
    public UserResponse updateProfile(String subject, UpdateProfileRequest request, MultipartFile file) {
        UUID id = Actors.id(subject);
        access.require(id);
        String key = images.upload("avatars/users/" + id + "/avatar", images.fromMultipart(file));
        return transaction.execute(status -> {
            var user = users.findLockedById(id).orElseThrow(UserAccess::missing);
            cleanup.enqueue(user.getAvatarObjectKey());
            user.updateAvatarObject(key);
            cleanup.completeUpload(key);
            return responses.from(user);
        });
    }
}
