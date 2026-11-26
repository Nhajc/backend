package com.nhajc.server.storage;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.nhajc.server.common.RecordingTransactions;
import com.nhajc.server.storage.entity.StorageCleanupJob;
import com.nhajc.server.storage.exception.ObjectStorageException;
import com.nhajc.server.storage.repository.StorageCleanupRepository;
import com.nhajc.server.storage.service.ObjectStorageService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

class ObjectStorageCleanupTest {
    final StorageCleanupRepository jobs = mock(StorageCleanupRepository.class);
    final ObjectStorageService storage = mock(ObjectStorageService.class);
    final RecordingTransactions manager = new RecordingTransactions();
    final ObjectStorageCleanup cleanup = new ObjectStorageCleanup(jobs, storage, manager);

    @Test
    void remoteDeletionRunsAfterClaimTransactionEnds() {
        var job = new StorageCleanupJob("abandoned.png", Instant.EPOCH);
        when(jobs.findDeletable(any(), any())).thenReturn(List.of(job));
        doAnswer(
                        call -> {
                            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                            return null;
                        })
                .when(storage)
                .delete("abandoned.png");
        doAnswer(
                        call -> {
                            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
                            return null;
                        })
                .when(jobs)
                .deleteClaimed("abandoned.png");
        cleanup.deleteUnusedObjects();
        var order = inOrder(jobs, storage);
        order.verify(jobs).findDeletable(any(), any());
        order.verify(storage).delete("abandoned.png");
        order.verify(jobs).deleteClaimed("abandoned.png");
    }

    @Test
    void claimedOrExpiredReservationCannotBeAttached() {
        when(jobs.completeUpload("claimed.png")).thenReturn(0);
        assertThatThrownBy(() ->
                        new TransactionTemplate(manager)
                                .executeWithoutResult(s -> cleanup.completeUpload("claimed.png")))
                .isInstanceOf(ObjectStorageException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void failedDeleteRetainsJobForRetry() {
        when(jobs.findDeletable(any(), any())).thenReturn(List.of(new StorageCleanupJob("retry", Instant.EPOCH)));
        doThrow(new ObjectStorageException("Unavailable")).when(storage).delete("retry");
        cleanup.deleteUnusedObjects();
        verify(jobs, never()).deleteClaimed(anyString());
    }

    @Test
    void imageUploadKeepsReservationUntilCallerPublishesPointer() {
        var images = new ImageStorage(storage, cleanup);
        doAnswer(
                        call -> {
                            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                            return null;
                        })
                .when(storage)
                .upload(anyString(), any(byte[].class), anyString());
        String key = images.upload("playlists/test/cover", new ImageStorage.Image(new byte[] {1}, "image/png"));
        assertThat(key).startsWith("playlists/test/cover.").endsWith(".png");
        verify(jobs).saveAndFlush(any());
        verify(jobs, never()).completeUpload(anyString());
    }
}
