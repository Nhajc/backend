package com.nhajc.server.storage;

import com.nhajc.server.storage.service.ObjectStorageService;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ImageStorage {
    private static final int MAX_BYTES = 5 * 1024 * 1024;
    private final ObjectStorageService storage;
    private final ObjectStorageCleanup cleanup;

    public ImageStorage(ObjectStorageService storage, ObjectStorageCleanup cleanup) {
        this.storage = storage;
        this.cleanup = cleanup;
    }

    public record Image(byte[] bytes, String contentType) {}

    public Image fromMultipart(MultipartFile file) {
        if (file == null) return null;
        if (file.getSize() > MAX_BYTES) throw tooLarge();
        try (var input = file.getInputStream()) {
            // Bound actual bytes as well as the advertised multipart size.
            return normalize(input.readNBytes(MAX_BYTES + 1), null);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read image file.", exception);
        }
    }

    public Image normalize(byte[] bytes, String expectedMime) {
        if (bytes.length > MAX_BYTES) throw tooLarge();
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalid();
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                String mime =
                        switch (format) {
                            case "png" -> "image/png";
                            case "jpeg" -> "image/jpeg";
                            default -> throw invalid();
                        };
                if (expectedMime != null && !mime.equals(expectedMime)) throw invalid();
                reader.setInput(input, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > 40_000_000) throw invalid();
                var param = reader.getDefaultReadParam();
                int sample = Math.max(1, Math.max(width, height) / 1200);
                param.setSourceSubsampling(sample, sample, 0, 0);
                BufferedImage source = reader.read(0, param);
                double scale = Math.min(1.0, 600.0 / Math.max(source.getWidth(), source.getHeight()));
                BufferedImage output =
                        new BufferedImage(
                                Math.max(1, (int) (source.getWidth() * scale)),
                                Math.max(1, (int) (source.getHeight() * scale)),
                                format.equals("png") ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
                var graphics = output.createGraphics();
                try {
                    graphics.setRenderingHint(
                            RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    graphics.drawImage(source, 0, 0, output.getWidth(), output.getHeight(), null);
                } finally {
                    graphics.dispose();
                }
                var encoded = new ByteArrayOutputStream();
                if (!ImageIO.write(output, format, encoded)) throw invalid();
                return new Image(encoded.toByteArray(), mime);
            } finally {
                reader.dispose();
            }
        } catch (IOException | IllegalArgumentException exception) {
            throw invalid();
        }
    }

    public String upload(String prefix, Image image) {
        return upload(prefix, image, null);
    }

    public String upload(String prefix, Image image, java.time.Duration gracePeriod) {
        if (image == null) return null;
        String key = prefix + "." + UUID.randomUUID() + (image.contentType().equals("image/png") ? ".png" : ".jpg");
        if (gracePeriod == null) cleanup.reserveUpload(key);
        else cleanup.reserveUpload(key, gracePeriod);
        storage.upload(key, image.bytes(), image.contentType());
        return key;
    }

    private static ResponseStatusException invalid() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Image must be a non-empty, valid JPEG/PNG file (at most 40 million pixels).");
    }

    private static ResponseStatusException tooLarge() {
        return new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "Image must be at most 5 MB.");
    }
}
