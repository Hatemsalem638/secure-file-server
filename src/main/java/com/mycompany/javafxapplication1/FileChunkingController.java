package com.mycompany.javafxapplication1;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits file content into a fixed number of chunks and joins them back.
 * Works on raw bytes, so encrypted (binary) content is preserved exactly.
 */
public final class FileChunkingController {

    private static final int NUM_CHUNKS = 4;

    private FileChunkingController() {
    }

    public static int getNumChunks() {
        return NUM_CHUNKS;
    }

    /** Always returns NUM_CHUNKS parts; trailing parts may be empty for very small files. */
    public static List<byte[]> split(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Content is null");
        }
        int chunkSize = (int) Math.ceil((double) data.length / NUM_CHUNKS);
        List<byte[]> chunks = new ArrayList<>(NUM_CHUNKS);
        for (int i = 0; i < NUM_CHUNKS; i++) {
            int start = Math.min(i * chunkSize, data.length);
            int end = Math.min(start + chunkSize, data.length);
            byte[] chunk = new byte[end - start];
            System.arraycopy(data, start, chunk, 0, chunk.length);
            chunks.add(chunk);
        }
        return chunks;
    }

    public static byte[] join(List<byte[]> chunks) {
        int total = chunks.stream().mapToInt(c -> c.length).sum();
        byte[] out = new byte[total];
        int pos = 0;
        for (byte[] c : chunks) {
            System.arraycopy(c, 0, out, pos, c.length);
            pos += c.length;
        }
        return out;
    }
}
