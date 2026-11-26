package com.nhajc.server.song.audio;

import static org.assertj.core.api.Assertions.*;

import java.io.*;
import javax.sound.sampled.*;
import org.junit.jupiter.api.Test;

class PcmNormalizationTest {
    @Test
    void convertsMono16kToStereo48kWithoutDiskWav() throws Exception {
        var wav = new ByteArrayOutputStream();
        try (var audio =
                new AudioInputStream(
                        new ByteArrayInputStream(new byte[32000]), new AudioFormat(16000, 16, 1, true, false), 16000)) {
            AudioSystem.write(audio, AudioFileFormat.Type.WAVE, wav);
        }
        var pcm = new ByteArrayOutputStream();
        FlacAudioEncoder.normalizePcm(
                new ByteArrayInputStream(wav.toByteArray()), pcm, new AudioFormat(48000, 16, 2, true, false), 200000);
        assertThat(pcm.size() / 4).isBetween(47990, 48010);
    }
}
