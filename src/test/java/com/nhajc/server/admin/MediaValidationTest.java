package com.nhajc.server.admin;

import com.nhajc.server.song.audio.FlacMetadata;
import com.nhajc.server.storage.ImageStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.server.ResponseStatusException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import org.springframework.mock.web.MockMultipartFile;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;

class MediaValidationTest {
    @TempDir Path directory;
    private final ImageStorage images = new ImageStorage(null, null);

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

    @Test
    void resizesImageAndStripsMetadata() throws Exception {
        var encoded = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1800, 900, BufferedImage.TYPE_INT_RGB), "png", encoded);
        var image = images.fromMultipart(new MockMultipartFile("avatar", "avatar.png", "image/png", encoded.toByteArray()));
        var decoded = ImageIO.read(new ByteArrayInputStream(image.bytes()));
        assertThat(decoded.getWidth()).isEqualTo(600);
        assertThat(decoded.getHeight()).isEqualTo(300);
        assertThat(image.contentType()).isEqualTo("image/png");
    }

    @Test
    void nullImageIsOptionalButEmptyOrInvalidFileIsRejected() throws Exception {
        assertThat(images.fromMultipart(null)).isNull();
        assertThatThrownBy(() -> images.fromMultipart(new MockMultipartFile("avatar", new byte[0])))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> images.fromMultipart(new MockMultipartFile("avatar", "bad.png", "image/png", new byte[]{1, 2})))
                .isInstanceOf(ResponseStatusException.class);
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
        assertThatThrownBy(() -> images.normalize(bytes.toByteArray(), "image/jpeg")).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void oversizedImageReturns413() {
        assertThatThrownBy(() -> images.normalize(new byte[5 * 1024 * 1024 + 1], "image/png"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> assertThat(exception.getStatusCode().value()).isEqualTo(413));
    }
}
