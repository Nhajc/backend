package com.nhajc.server.song.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.*;

@Component
public class FlacAudioEncoder implements AudioEncoder {
    private static final Logger log = LoggerFactory.getLogger(FlacAudioEncoder.class);
    private final String flac;
    private final String fdkaac;
    private final int timeoutSeconds;

    public FlacAudioEncoder(@Value("${audio.flac-command:flac}") String flac,
                            @Value("${audio.fdkaac-command:fdkaac}") String fdkaac,
                            @Value("${audio.command-timeout-seconds:180}") int timeoutSeconds) {
        if (timeoutSeconds < 1 || timeoutSeconds > 600) throw new IllegalArgumentException("Audio timeout must be 1–600 seconds.");
        this.flac = flac; this.fdkaac = fdkaac; this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public Map<String, Path> encode(Path original, Path directory, FlacMetadata metadata) {
        Path wav = directory.resolve("decoded.wav");
        Path normalized = directory.resolve("normalized.wav");
        Path outputLog = directory.resolve("encoder.log");
        long pcmBytes = metadata.samples() * metadata.channels() * 2;
        try {
            run(List.of(flac, "--decode", "--silent", "--force", "--output-name=" + wav, original.toString()),
                    wav, pcmBytes + 65536, outputLog, true);
            // WAV contains only the decoded PCM plus a small RIFF header.
            long decodedSize = Files.size(wav);
            if (decodedSize < pcmBytes || decodedSize > pcmBytes + 65536) throw FlacMetadata.invalid();
            Path aacInput = wav;
            // AAC profiles have narrower sample-rate/bitrate combinations than FLAC.
            // Convert uncommon rates and mono to 48 kHz stereo for all requested profiles.
            if (metadata.channels() != 2 || metadata.sampleRate() != 44100 && metadata.sampleRate() != 48000) {
                normalizePcm(wav, normalized);
                aacInput = normalized;
            }
            Map<String, Path> files = new LinkedHashMap<>();
            String[] qualities = {"low", "medium", "high"};
            int[] bitrates = {64000, 128000, 256000};
            for (int i = 0; i < qualities.length; i++) {
                Path output = directory.resolve(qualities[i] + ".m4a");
                run(List.of(fdkaac, "--silent", "--profile", i == 0 ? "5" : "2", "--bitrate",
                        Integer.toString(bitrates[i]), "-o", output.toString(), aacInput.toString()),
                        output, 300L * 1024 * 1024, outputLog, false);
                files.put(qualities[i], output);
            }
            files.put("lossless", original);
            return files;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Audio processing is temporarily unavailable.", exception);
        } finally {
            for (Path temporary : List.of(wav, normalized)) {
                try { Files.deleteIfExists(temporary); }
                catch (IOException exception) { log.warn("Unable to delete temporary WAV {}", temporary); }
            }
        }
    }

    static void normalizePcm(Path input, Path output) throws IOException {
        AudioFormat target = new AudioFormat(48000, 16, 2, true, false);
        try (var source = AudioSystem.getAudioInputStream(input.toFile());
             var converted = AudioSystem.getAudioInputStream(target, source)) {
            AudioSystem.write(converted, AudioFileFormat.Type.WAVE, output.toFile());
        } catch (UnsupportedAudioFileException | IllegalArgumentException exception) {
            throw FlacMetadata.invalid();
        }
    }

    private void run(List<String> command, Path output, long maxBytes, Path outputLog, boolean decoding) throws IOException {
        Process process;
        try {
            // Individual arguments, no shell; client filenames are never used in commands.
            process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(outputLog.toFile()).start();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Audio tools unavailable. Configure AUDIO_FLAC_COMMAND and AUDIO_FDKAAC_COMMAND or run the Docker server.", exception);
        }
        try {
            process.getOutputStream().close();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
            while (!process.waitFor(200, TimeUnit.MILLISECONDS)) {
                if (Files.exists(output) && Files.size(output) > maxBytes) throw FlacMetadata.invalid();
                if (Files.exists(outputLog) && Files.size(outputLog) > 1024 * 1024) throw FlacMetadata.invalid();
                if (System.nanoTime() >= deadline) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Audio processing timed out. Please retry.");
            }
            if (process.exitValue() != 0 || !Files.exists(output) || Files.size(output) == 0 || Files.size(output) > maxBytes) {
                log.warn("Audio {} failed, exit code {}", decoding ? "decoding" : "encoding", process.exitValue());
                if (decoding) throw FlacMetadata.invalid();
                if (Files.exists(outputLog) && Files.size(outputLog) < 1024 * 1024
                        && Files.readString(outputLog).contains("unsupported profile")) {
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "The installed FDK AAC library does not support HE-AAC. Install a full SBR-enabled build or use the Docker server.");
                }
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, "Audio cannot be encoded with the requested AAC profiles.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Audio processing interrupted.", exception);
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                try { process.waitFor(5, TimeUnit.SECONDS); }
                catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
            }
        }
    }
}
