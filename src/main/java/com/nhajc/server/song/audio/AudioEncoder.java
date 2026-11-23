package com.nhajc.server.song.audio;

import java.nio.file.Path;
import java.util.Map;

public interface AudioEncoder {
    Map<String, Path> encode(Path original, Path directory, FlacMetadata metadata);
}
