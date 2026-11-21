package com.nhajc.server.user;

import com.nhajc.server.storage.exception.ObjectStorageException;
import com.nhajc.server.user.repository.UserRepository;
import com.nhajc.server.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LegacyAvatarMigration {
    private static final Logger log = LoggerFactory.getLogger(LegacyAvatarMigration.class);

    private final UserRepository users;
    private final UserService userService;

    public LegacyAvatarMigration(UserRepository users, UserService userService) {
        this.users = users;
        this.userService = userService;
    }

    @Scheduled(initialDelayString = "${storage.avatar-migration-delay-ms:5000}",
            fixedDelayString = "${storage.avatar-migration-interval-ms:60000}")
    public void migrateAvatars() {
        for (var userId : users.findLegacyAvatarIds()) {
            try {
                userService.migrateLegacyAvatar(userId);
            } catch (ObjectStorageException exception) {
                log.warn("Legacy avatar migration will retry for user {}", userId);
                return;
            }
        }
    }
}
