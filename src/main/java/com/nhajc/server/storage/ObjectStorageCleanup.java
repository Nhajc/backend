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
        if (key != null && jobs.completeUpload(key) != 1) {
            throw new ObjectStorageException("Upload reservation expired. Please retry.");
        }
    }

    public void enqueue(String key) {
        // The caller holds the owner's write lock; each stored object key is unique.
        if (key != null && !jobs.existsById(key)) {
            jobs.save(new StorageCleanupJob(key, Instant.now()));
        }
    }

    @Scheduled(fixedDelayString = "${storage.cleanup-delay-ms:30000}")
    public void deleteUnusedObjects() {
        var keys = independentTransaction.execute(status -> {
            var batch = jobs.findDeletable(Instant.now(), PageRequest.of(0, 20));
            Instant retryAfter = Instant.now().plusSeconds(60);
            batch.forEach(job -> job.claimUntil(retryAfter));
            return batch.stream().map(StorageCleanupJob::getObjectKey).toList();
        });
        if (keys == null) return;
        for (String key : keys) {
            try {
                storage.delete(key);
                independentTransaction.executeWithoutResult(status -> jobs.deleteClaimed(key));
            } catch (ObjectStorageException exception) {
                log.warn("Object cleanup will retry for {}", key);
            }
        }
    }
}
