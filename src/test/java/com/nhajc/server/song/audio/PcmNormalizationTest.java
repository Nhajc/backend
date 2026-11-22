package com.nhajc.server.song.audio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class PcmNormalizationTest {
    @TempDir Path directory;

    @Test void convertsMono16kToStereo48kWithoutChangingDuration() throws Exception {
        Path input = directory.resolve("mono.wav"), output = directory.resolve("stereo.wav");
        AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
        try (var audio = new AudioInputStream(new ByteArrayInputStream(new byte[32000]), format, 16000)) {
            AudioSystem.write(audio, AudioFileFormat.Type.WAVE, input.toFile());
        }
        FlacAudioEncoder.normalizePcm(input, output);
        try (var result = AudioSystem.getAudioInputStream(output.toFile())) {
            assertThat(result.getFormat().getSampleRate()).isEqualTo(48000);
            assertThat(result.getFormat().getChannels()).isEqualTo(2);
            assertThat(result.getFrameLength()).isBetween(47990L, 48010L);
        }
    }
}
