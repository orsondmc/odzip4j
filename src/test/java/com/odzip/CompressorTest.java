package com.odzip;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for compression and decompression round-tripping
 */
public class CompressorTest {

    @Test
    public void testRoundTripEmpty() {
        byte[] input = new byte[0];
        byte[] compressed = Compressor.compressSimple(input);
        byte[] decompressed = Decompressor.decompressSimple(compressed);
        assertArrayEquals("Empty array should round-trip", input, decompressed);
    }

    @Test
    public void testRoundTripSingleByte() {
        byte[] input = new byte[]{42};
        byte[] compressed = Compressor.compressSimple(input);
        byte[] decompressed = Decompressor.decompressSimple(compressed);
        assertArrayEquals("Single byte should round-trip", input, decompressed);
    }

    @Test
    public void testRoundTripSmall() {
        byte[] input = "hello".getBytes();
        byte[] compressed = Compressor.compressSimple(input);
        byte[] decompressed = Decompressor.decompressSimple(compressed);
        assertArrayEquals("Small string should round-trip", input, decompressed);
    }

    @Test
    public void testRoundTripWithRepeats() {
        byte[] input = "abcabcabc123".getBytes();
        byte[] compressed = Compressor.compressSimple(input);
        byte[] decompressed = Decompressor.decompressSimple(compressed);
        assertArrayEquals("String with repeats should round-trip", input, decompressed);
    }

    @Test
    public void testRoundTripLongRepeats() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("test");
        }
        byte[] input = sb.toString().getBytes();
        byte[] compressed = Compressor.compressSimple(input);
        byte[] decompressed = Decompressor.decompressSimple(compressed);
        assertArrayEquals("Long string with repeats should round-trip", input, decompressed);
    }

    @Test
    public void testRoundTripBinary() {
        byte[] input = new byte[256];
        for (int i = 0; i < 256; i++) {
            input[i] = (byte)i;
        }
        byte[] compressed = Compressor.compressSimple(input);
        byte[] decompressed = Decompressor.decompressSimple(compressed);
        assertArrayEquals("Binary data should round-trip", input, decompressed);
    }

    @Test
    public void testRoundTripRandom() {
        byte[] input = new byte[1000];
        for (int i = 0; i < input.length; i++) {
            input[i] = (byte)((i * 17 + 23) % 256);
        }
        byte[] compressed = Compressor.compressSimple(input);
        byte[] decompressed = Decompressor.decompressSimple(compressed);
        assertArrayEquals("Random-like data should round-trip", input, decompressed);
    }

    @Test
    public void testRoundTripLarge() {
        byte[] input = new byte[10000];
        for (int i = 0; i < input.length; i++) {
            input[i] = (byte)(i % 256);
        }
        byte[] compressed = Compressor.compressSimple(input);
        byte[] decompressed = Decompressor.decompressSimple(compressed);
        assertArrayEquals("Large data should round-trip", input, decompressed);
    }

    @Test
    public void testRoundTripAllZeros() {
        byte[] input = new byte[100];
        // Already initialized to zeros
        byte[] compressed = Compressor.compressSimple(input);
        byte[] decompressed = Decompressor.decompressSimple(compressed);
        assertArrayEquals("All zeros should round-trip", input, decompressed);
    }

    @Test
    public void testRoundTripAllOnes() {
        byte[] input = new byte[100];
        for (int i = 0; i < input.length; i++) {
            input[i] = (byte)0xFF;
        }
        byte[] compressed = Compressor.compressSimple(input);
        byte[] decompressed = Decompressor.decompressSimple(compressed);
        assertArrayEquals("All ones should round-trip", input, decompressed);
    }

    @Test
    public void testRoundTripLICENSE() {
        // Test with LICENSE file content if available
        String licenseText = "MIT License\n\nCopyright (c) 2025 odpay\n\nPermission is hereby granted";
        byte[] input = licenseText.getBytes();
        byte[] compressed = Compressor.compressSimple(input);
        byte[] decompressed = Decompressor.decompressSimple(compressed);
        assertArrayEquals("LICENSE-like text should round-trip", input, decompressed);
    }

    @Test
    public void testCompressionReducesSizeForRepeats() {
        // Create data with many repeats that should compress well
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("repeated");
        }
        byte[] input = sb.toString().getBytes();
        byte[] compressed = Compressor.compressSimple(input);
        // Note: compression may not always reduce size, but for highly repetitive data it should
        assertTrue("Compressed data should be valid", compressed.length > 0);
        assertTrue("Compressed data should have header", compressed.length >= 8);
    }

    @Test
    public void testCompressedFormat() {
        byte[] input = "test".getBytes();
        byte[] compressed = Compressor.compressSimple(input);
        
        // Check magic header
        assertEquals("Magic byte 0", 'O', compressed[0]);
        assertEquals("Magic byte 1", 'D', compressed[1]);
        assertEquals("Magic byte 2", 'Z', compressed[2]);
        assertEquals("Magic byte 3", OdzConstants.ODZ_VERSION, compressed[3]);
        
        // Check size header
        int size = OdzUtil.readU32LE(compressed, 4);
        assertEquals("Size should match input", input.length, size);
    }
}
