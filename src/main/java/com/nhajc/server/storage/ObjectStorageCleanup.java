package com.nhajc.server.storage;

import com.nhajc.server.storage.exception.ObjectStorageException;
import com.nhajc.server.storage.service.ObjectStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.nhajc.server.storage.entity.StorageCleanupJob;
import com.nhajc.server.storage.repository.StorageCleanupRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;

@Component
public class ObjectStorageCleanup {
    private static final Logger log = LoggerFactory.getLogger(ObjectStorageCleanup.class);

    private final StorageCleanupRepository jobs;
    private final ObjectStorageService storage;
    private final TransactionTemplate independentTransaction;

    public ObjectStorageCleanup(StorageCleanupRepository jobs, ObjectStorageService storage,
                                PlatformTransactionManager transactionManager) {
        this.jobs = jobs;
        this.storage = storage;
        this.independentTransaction = new TransactionTemplate(transactionManager);
        this.independentTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void reserveUpload(String key) {
        reserveUpload(key, java.time.Duration.ofMinutes(15));
    }

    public void reserveUpload(String key, java.time.Duration gracePeriod) {
        // Allow in-flight uploads to finish before considering an unreferenced object abandoned.
        independentTransaction.executeWithoutResult(status ->
                jobs.saveAndFlush(new StorageCleanupJob(key, Instant.now().plus(gracePeriod))));
    }

    public void completeUpload(String key) {
        // Joins the user update transaction, so a rollback restores the cleanup reservation.
        jobs.deleteByObjectKey(key);
    }

    public void enqueue(String key) {
        // The caller holds the owner's write lock; each stored object key is unique.
        if (key != null && !jobs.existsById(key)) {
            jobs.save(new StorageCleanupJob(key, Instant.now()));
        }
    }

    @Scheduled(fixedDelayString = "${storage.cleanup-delay-ms:30000}")
    public void deleteUnusedObjects() {
        independentTransaction.executeWithoutResult(status -> {
            var batch = jobs.findDeletable(Instant.now(), PageRequest.of(0, 20));
            for (StorageCleanupJob job : batch) {
                try {
                    storage.delete(job.getObjectKey());
                    jobs.delete(job);
                } catch (ObjectStorageException exception) {
                    log.warn("Object cleanup will retry for {}", job.getObjectKey());
                }
            }
        });
    }
}
