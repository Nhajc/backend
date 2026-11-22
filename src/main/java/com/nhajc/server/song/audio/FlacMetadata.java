package com.nhajc.server.song.audio;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public record FlacMetadata(int sampleRate, int channels, int bitsPerSample, long samples,
                           byte[] picture, String pictureMime) {
    public int durationSeconds() { return (int) ((samples + sampleRate - 1) / sampleRate); }

    public static FlacMetadata read(Path file) throws IOException {
        try (var in = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            if (in.readInt() != 0x664c6143) throw invalid();
            int header = in.readUnsignedByte();
            int length = length(in);
            if ((header & 127) != 0 || length != 34) throw invalid();
            byte[] info = in.readNBytes(34);
            if (info.length != 34) throw invalid();
            long packed = 0;
            for (int i = 10; i < 18; i++) packed = (packed << 8) | (info[i] & 255L);
            int rate = (int) (packed >>> 44);
            int channels = (int) ((packed >>> 41) & 7) + 1;
            int bits = (int) ((packed >>> 36) & 31) + 1;
            long samples = packed & 0xfffffffffL;
            if (rate <= 0 || rate > 48000 || channels > 2 || bits != 16 || samples == 0
                    || samples > rate * 7200L) throw invalid();
            byte[] picture = null;
            String mime = null;
            long metadataBytes = 34;
            int pictureType = -1;
            while ((header & 128) == 0) {
                header = in.readUnsignedByte(); length = length(in);
                metadataBytes += length + 4L;
                if (metadataBytes > 32 * 1024 * 1024 || (header & 127) == 0 || (header & 127) == 127) throw invalid();
                if ((header & 127) != 6 || length > 5 * 1024 * 1024 + 65536) {
                    in.skipNBytes(length); continue;
                }
                byte[] block = in.readNBytes(length);
                if (block.length != length) throw invalid();
                try (var data = new DataInputStream(new ByteArrayInputStream(block))) {
                    int type = data.readInt();
                    int mimeLength = data.readInt();
                    if (mimeLength < 0 || mimeLength > data.available()) throw invalid();
                    String candidateMime = new String(data.readNBytes(mimeLength), StandardCharsets.US_ASCII);
                    int descriptionLength = data.readInt();
                    if (descriptionLength < 0 || descriptionLength > data.available() - 20) throw invalid();
                    data.skipNBytes(descriptionLength);
                    data.skipNBytes(16); // width, height, depth, palette size
                    int size = data.readInt();
                    if (size < 0 || size != data.available()) throw invalid();
                    if (size <= 5 * 1024 * 1024 && (candidateMime.equals("image/png") || candidateMime.equals("image/jpeg"))
                            && (picture == null || type == 3 && pictureType != 3)) {
                        picture = data.readNBytes(size); mime = candidateMime; pictureType = type;
                    }
                }
            }
            if (in.read() == -1) throw invalid();
            return new FlacMetadata(rate, channels, bits, samples, picture, mime);
        } catch (EOFException exception) { throw invalid(); }
    }

    private static int length(DataInputStream in) throws IOException {
        return (in.readUnsignedByte() << 16) | (in.readUnsignedByte() << 8) | in.readUnsignedByte();
    }

    public static ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Audio must be a valid native FLAC: 16-bit, at most 48 kHz, mono/stereo, duration greater than zero and at most 7200 seconds.");
    }
}
