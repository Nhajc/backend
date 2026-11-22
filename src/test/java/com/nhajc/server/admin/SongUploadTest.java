package com.nhajc.server.admin;

import com.nhajc.server.release.entity.*;
import com.nhajc.server.release.repository.ReleaseRepository;
import com.nhajc.server.song.audio.AudioEncoder;
import com.nhajc.server.song.dto.UploadSongRequest;
import com.nhajc.server.song.repository.SongRepository;
import com.nhajc.server.song.service.impl.SongServiceImpl;
import com.nhajc.server.storage.*;
import com.nhajc.server.storage.service.ObjectStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SongUploadTest {
    private SongRepository songs;
    private ReleaseRepository releases;
    private AudioEncoder encoder;
    private ObjectStorageService storage;
    private ObjectStorageCleanup cleanup;
    private SongServiceImpl service;
    private final UUID releaseId = UUID.randomUUID();

    @BeforeEach void setUp() {
        songs = mock(SongRepository.class); releases = mock(ReleaseRepository.class);
        encoder = mock(AudioEncoder.class); storage = mock(ObjectStorageService.class);
        cleanup = mock(ObjectStorageCleanup.class);
        var manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        service = new SongServiceImpl(songs, releases, encoder, mock(ImageStorage.class), storage, cleanup, manager);
        when(releases.existsById(releaseId)).thenReturn(true);
        when(releases.findLockedById(releaseId)).thenReturn(Optional.of(new Release(releaseId, "Album", ReleaseType.ALBUM, LocalDate.now(), null, Set.of(), Set.of())));
        when(encoder.encode(any(), any(), any())).thenAnswer(invocation -> {
            Path path = invocation.getArgument(0);
            return Map.of("low", path, "medium", path, "high", path, "lossless", path);
        });
    }

    private UploadSongRequest request(int track) {
        return new UploadSongRequest(track, " Song ", new MockMultipartFile("file", "song.flac", "audio/flac", MediaFixtures.flac(44100, 2, 16, 44100)));
    }

    @Test void publishesOnlyAfterAllUploadsAndClearsReservations() {
        UUID id = service.upload(releaseId, request(1));
        var order = inOrder(storage, songs);
        order.verify(storage, times(4)).upload(anyString(), any(Path.class), anyString());
        order.verify(songs).saveAndFlush(argThat(song -> song.getId().equals(id) && song.getDurationSeconds() == 1 && song.getAudio().size() == 4));
        verify(cleanup, times(4)).completeUpload(anyString());
    }

    @Test void failedStorageNeverPublishesAndLeavesCleanupReservations() {
        doThrow(new IllegalStateException("storage unavailable")).when(storage).upload(anyString(), any(Path.class), anyString());
        assertThatThrownBy(() -> service.upload(releaseId, request(1))).isInstanceOf(IllegalStateException.class);
        verify(songs, never()).saveAndFlush(any());
        verify(cleanup).reserveUpload(anyString(), any());
        verify(cleanup, never()).completeUpload(anyString());
    }

    @Test void duplicateRaceAfterEncodingDoesNotPublishOrCancelCleanup() {
        when(songs.existsByRelease_IdAndTrackNumber(releaseId, 1)).thenReturn(false, true);
        assertThatThrownBy(() -> service.upload(releaseId, request(1)))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> assertThat(exception.getStatusCode().value()).isEqualTo(409));
        verify(songs, never()).saveAndFlush(any());
        verify(cleanup, never()).completeUpload(anyString());
    }

    @Test void rejectsThirdConcurrentEncodingAndReleasesSlotsAfterFailure() throws Exception {
        var started = new CountDownLatch(2);
        var finish = new CountDownLatch(1);
        doAnswer(invocation -> {
            started.countDown();
            if (!finish.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Test synchronization timed out");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad audio");
        }).when(encoder).encode(any(), any(), any());
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> service.upload(releaseId, request(1)));
            var second = executor.submit(() -> service.upload(releaseId, request(2)));
            try {
                assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> service.upload(releaseId, request(3)))
                        .isInstanceOfSatisfying(ResponseStatusException.class, exception -> assertThat(exception.getStatusCode().value()).isEqualTo(429));
            } finally { finish.countDown(); }
            assertThatThrownBy(() -> first.get(5, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class);
            assertThatThrownBy(() -> second.get(5, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class);
        }
        assertThatThrownBy(() -> service.upload(releaseId, request(4)))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> assertThat(exception.getStatusCode().value()).isEqualTo(400));
    }
}
