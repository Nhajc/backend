package com.nhajc.server.storage;

import com.nhajc.server.storage.ImageStorage;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import org.springframework.mock.web.MockMultipartFile;
import static org.assertj.core.api.Assertions.*;

class ImageStorageTest {
    private final ImageStorage images = new ImageStorage(null, null);

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
