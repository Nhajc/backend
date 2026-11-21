package com.nhajc.server.storage;

import com.nhajc.server.storage.exception.ObjectStorageException;
import com.nhajc.server.storage.service.ObjectStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class ObjectStorageCleanup {
    private static final Logger log = LoggerFactory.getLogger(ObjectStorageCleanup.class);

    private final JdbcTemplate jdbc;
    private final ObjectStorageService storage;
    private final TransactionTemplate independentTransaction;

    public ObjectStorageCleanup(JdbcTemplate jdbc, ObjectStorageService storage,
                                PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.storage = storage;
        this.independentTransaction = new TransactionTemplate(transactionManager);
        this.independentTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void reserveUpload(String key) {
        // Allow in-flight uploads to finish before considering an unreferenced object abandoned.
        independentTransaction.executeWithoutResult(status -> jdbc.update("""
                INSERT INTO storage_object_cleanup (object_key, delete_after)
                VALUES (?, CURRENT_TIMESTAMP + INTERVAL '15 minutes')
                """, key));
    }

    public void completeUpload(String key) {
        // Joins the user update transaction, so a rollback restores the cleanup reservation.
        jdbc.update("DELETE FROM storage_object_cleanup WHERE object_key = ?", key);
    }

    public void enqueue(String key) {
        if (key != null) {
            jdbc.update("""
                    INSERT INTO storage_object_cleanup (object_key) VALUES (?)
                    ON CONFLICT (object_key) DO NOTHING
                    """, key);
        }
    }

    @Scheduled(fixedDelayString = "${storage.cleanup-delay-ms:30000}")
    public void deleteUnusedObjects() {
        independentTransaction.executeWithoutResult(status -> {
            var keys = jdbc.queryForList("""
                    SELECT c.object_key FROM storage_object_cleanup c
                    WHERE c.delete_after <= CURRENT_TIMESTAMP
                      AND NOT EXISTS (SELECT 1 FROM users u WHERE u.avatar_object_key = c.object_key)
                    ORDER BY c.delete_after
                    LIMIT 20 FOR UPDATE OF c SKIP LOCKED
                    """, String.class);
            for (String key : keys) {
                try {
                    storage.delete(key);
                    jdbc.update("DELETE FROM storage_object_cleanup WHERE object_key = ?", key);
                } catch (ObjectStorageException exception) {
                    log.warn("Object cleanup will retry for {}", key);
                }
            }
        });
    }
}
