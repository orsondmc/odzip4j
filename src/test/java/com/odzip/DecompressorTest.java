package com.odzip;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for decompression round-tripping
 */
public class DecompressorTest {

    @Test
    public void testRoundTripVariousSizes() {
        // Test various sizes to ensure they all work
        for (int size : new int[]{1, 2, 3, 7, 8, 9, 15, 16, 17, 100, 255, 256, 1000}) {
            byte[] input = new byte[size];
            for (int i = 0; i < size; i++) {
                input[i] = (byte)(i % 256);
            }
            byte[] compressed = Compressor.compressSimple(input);
            byte[] decompressed = Decompressor.decompressSimple(compressed);
            assertArrayEquals("Size " + size + " should round-trip", input, decompressed);
        }
    }

    @Test
    public void testRoundTripExactMatchBoundaries() {
        // Test sizes that are exact multiples of 8 (token group size)
        for (int size : new int[]{8, 16, 24, 32, 64, 128, 256}) {
            byte[] input = new byte[size];
            for (int i = 0; i < size; i++) {
                input[i] = (byte)((i * 7 + 13) % 256);
            }
            byte[] compressed = Compressor.compressSimple(input);
            byte[] decompressed = Decompressor.decompressSimple(compressed);
            assertArrayEquals("Size " + size + " (exact match) should round-trip", input, decompressed);
        }
    }
}
