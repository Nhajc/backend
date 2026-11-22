package com.nhajc.server.admin;

import java.nio.ByteBuffer;

final class MediaFixtures {
    private MediaFixtures() {}
    static byte[] flac(int rate, int channels, int bits, long samples) {
        byte[] bytes = new byte[43];
        ByteBuffer.wrap(bytes).putInt(0x664c6143).put((byte) 128).put(new byte[]{0, 0, 34});
        long packed = ((long) rate << 44) | ((long) (channels - 1) << 41) | ((long) (bits - 1) << 36) | samples;
        ByteBuffer.wrap(bytes, 18, 8).putLong(packed);
        bytes[42] = (byte) 255;
        return bytes;
    }
}
