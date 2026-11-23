package com.nhajc.server.song.audio;
import com.nhajc.server.common.MediaFixtures;

import com.nhajc.server.song.audio.FlacMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.*;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;

class FlacMetadataTest {
    @TempDir Path directory;

    @Test
    void acceptsBoundariesAndRoundsDurationUp() throws Exception {
        var file = directory.resolve("valid.flac");
        Files.write(file, MediaFixtures.flac(48000, 2, 16, 48000L * 7200));
        assertThat(FlacMetadata.read(file).durationSeconds()).isEqualTo(7200);
        Files.write(file, MediaFixtures.flac(44100, 1, 16, 44101));
        assertThat(FlacMetadata.read(file).durationSeconds()).isEqualTo(2);
    }

    static Stream<byte[]> invalidFlac() {
        return Stream.of(new byte[]{1, 2}, MediaFixtures.flac(96000, 2, 16, 96000),
                MediaFixtures.flac(44100, 6, 16, 44100), MediaFixtures.flac(44100, 2, 24, 44100),
                MediaFixtures.flac(44100, 2, 16, 0), MediaFixtures.flac(44100, 2, 16, 44100L * 7200 + 1),
                MediaFixtures.flac(0, 2, 16, 10));
    }
    @ParameterizedTest @MethodSource("invalidFlac")
    void rejectsUnsupportedOrTruncatedFlac(byte[] bytes) throws Exception {
        Path file = directory.resolve("invalid.flac"); Files.write(file, bytes);
        assertThatThrownBy(() -> FlacMetadata.read(file)).isInstanceOf(ResponseStatusException.class);
    }

}
