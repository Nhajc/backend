package com.nhajc.server.user;

import com.nhajc.server.storage.ObjectStorageCleanup;
import com.nhajc.server.storage.service.ObjectStorageService;
import com.nhajc.server.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserAvatarStorage {
    private final ObjectStorageService storage;
    private final ObjectStorageCleanup cleanup;

    public UserAvatarStorage(ObjectStorageService storage, ObjectStorageCleanup cleanup) {
        this.storage = storage;
        this.cleanup = cleanup;
    }

    // Call inside a transaction while holding the user's write lock.
    public void replace(User user, byte[] avatar, String contentType) {
        String oldKey = user.getAvatarObjectKey();
        String newKey = null;
        if (avatar != null) {
            String extension = "image/png".equals(contentType) ? ".png" : ".jpg";
            newKey = "avatars/users/" + user.getId() + "/" + UUID.randomUUID() + extension;
            cleanup.reserveUpload(newKey);
            storage.upload(newKey, avatar, contentType);
            cleanup.completeUpload(newKey);
        }
        user.updateAvatarObject(newKey);
        cleanup.enqueue(oldKey);
    }
}
