package com.nhajc.server.song.audio;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class FlacAudioEncoder implements AudioEncoder {
    private static final long MAX_OUTPUT_BYTES = 300L * 1024 * 1024;
    private static final long MAX_LOG_BYTES = 1024 * 1024;
    private final String flac;
    private final String fdkaac;
    private final int timeoutSeconds;

    public FlacAudioEncoder(
            @Value("${audio.flac-command:flac}") String flac,
            @Value("${audio.fdkaac-command:fdkaac}") String fdkaac,
            @Value("${audio.command-timeout-seconds:180}") int timeoutSeconds) {
        if (timeoutSeconds < 1 || timeoutSeconds > 600) {
            throw new IllegalArgumentException("Audio timeout must be 1–600 seconds.");
        }
        this.flac = flac;
        this.fdkaac = fdkaac;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public Map<String, Path> encode(Path original, Path directory, FlacMetadata metadata) {
        Map<String, Path> files = new LinkedHashMap<>();
        String[] qualities = {"low", "medium", "high"};
        int[] bitrates = {64000, 128000, 256000};
        try {
            for (int index = 0; index < qualities.length; index++) {
                Path output = directory.resolve(qualities[index] + ".m4a");
                encodeQuality(original, output, directory, metadata, index == 0 ? 5 : 2, bitrates[index]);
                files.put(qualities[index], output);
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Audio processing is temporarily unavailable.", exception);
        }
        files.put("lossless", original);
        return files;
    }

    private void encodeQuality(
            Path original, Path output, Path directory, FlacMetadata metadata, int profile, int bitrate)
            throws IOException {
        Path decoderLog = directory.resolve(output.getFileName() + ".decode.log");
        Path encoderLog = directory.resolve(output.getFileName() + ".encode.log");
        float rate =
                metadata.channels() == 2 && (metadata.sampleRate() == 44100 || metadata.sampleRate() == 48000)
                        ? metadata.sampleRate()
                        : 48000;
        AudioFormat target = new AudioFormat(rate, 16, 2, true, false);
        long maxPcmBytes = metadata.durationSeconds() * (long) rate * 4 + 65536;
        Process decoder = null;
        Process encoder = null;
        FutureTask<Void> transfer = null;
        try {
            decoder = new ProcessBuilder(flac, "-dcs", original.toString()).redirectError(decoderLog.toFile()).start();
            decoder.getOutputStream().close();
            encoder =
                    new ProcessBuilder(
                                    fdkaac,
                                    "--silent",
                                    "--moov-before-mdat",
                                    "--raw",
                                    "--raw-rate",
                                    Integer.toString((int) rate),
                                    "--raw-channels",
                                    "2",
                                    "--raw-format",
                                    "S16L",
                                    "--profile",
                                    Integer.toString(profile),
                                    "--bitrate",
                                    Integer.toString(bitrate),
                                    "-o",
                                    output.toString(),
                                    "-")
                            .redirectError(encoderLog.toFile())
                            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                            .start();
            Process source = decoder;
            Process destination = encoder;
            transfer =
                    new FutureTask<>(() -> {
                        try (var input = new BufferedInputStream(source.getInputStream());
                                var sink = destination.getOutputStream()) {
                            normalizePcm(input, sink, target, maxPcmBytes);
                        }
                        return null;
                    });
            Thread.ofVirtual().start(transfer);
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
            while (decoder.isAlive() || encoder.isAlive() || !transfer.isDone()) {
                if (System.nanoTime() >= deadline) {
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Audio processing timed out.");
                }
                if (oversized(output, MAX_OUTPUT_BYTES)
                        || oversized(decoderLog, MAX_LOG_BYTES)
                        || oversized(encoderLog, MAX_LOG_BYTES)) {
                    throw FlacMetadata.invalid();
                }
                if (transfer.isDone()) {
                    if (!encoder.isAlive() && encoder.exitValue() != 0) throw encodingFailure(encoderLog);
                    awaitTransfer(transfer);
                }
                Thread.sleep(100);
            }
            if (encoder.exitValue() != 0) throw encodingFailure(encoderLog);
            if (decoder.exitValue() != 0) throw FlacMetadata.invalid();
            awaitTransfer(transfer);
            if (!Files.exists(output) || Files.size(output) == 0 || oversized(output, MAX_OUTPUT_BYTES)) {
                throw encodingFailure(encoderLog);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Audio processing interrupted.", exception);
        } finally {
            stop(decoder);
            stop(encoder);
            if (transfer != null) transfer.cancel(true);
        }
    }

    static void normalizePcm(InputStream input, OutputStream output, AudioFormat target, long maxBytes)
            throws IOException {
        try (var source = AudioSystem.getAudioInputStream(input);
                var converted = AudioSystem.getAudioInputStream(target, source)) {
            byte[] buffer = new byte[65536];
            long total = 0;
            int count;
            while ((count = converted.read(buffer)) != -1) {
                total += count;
                if (total > maxBytes) throw FlacMetadata.invalid();
                output.write(buffer, 0, count);
            }
            if (total == 0) throw FlacMetadata.invalid();
        } catch (UnsupportedAudioFileException | IllegalArgumentException exception) {
            throw FlacMetadata.invalid();
        }
    }

    private static boolean oversized(Path file, long limit) throws IOException {
        return Files.exists(file) && Files.size(file) > limit;
    }

    private static void awaitTransfer(FutureTask<Void> transfer) throws InterruptedException {
        try {
            transfer.get();
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof ResponseStatusException response) throw response;
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "Unable to stream decoded audio to the encoder.",
                    exception.getCause());
        }
    }

    private static ResponseStatusException encodingFailure(Path log) throws IOException {
        if (Files.exists(log)
                && Files.size(log) <= MAX_LOG_BYTES
                && Files.readString(log).contains("unsupported profile")) {
            return new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "The installed FDK AAC library does not support HE-AAC.");
        }
        return new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_CONTENT, "Audio cannot be encoded with the requested AAC profile.");
    }

    private static void stop(Process process) {
        if (process == null || !process.isAlive()) return;
        process.destroyForcibly();
        try {
            process.waitFor(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
