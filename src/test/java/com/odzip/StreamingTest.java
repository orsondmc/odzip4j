package com.odzip;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Tests for streaming compression and decompression
 */
public class StreamingTest {

    @Test
    public void testCompressDecompressStreams() throws IOException {
        byte[] input = "Hello, World! This is a test string.".getBytes();
        
        // Compress using streams
        ByteArrayOutputStream compressedOut = new ByteArrayOutputStream();
        Compressor.compress(new ByteArrayInputStream(input), compressedOut);
        byte[] compressed = compressedOut.toByteArray();
        
        // Decompress using streams
        ByteArrayOutputStream decompressedOut = new ByteArrayOutputStream();
        Decompressor.decompress(new ByteArrayInputStream(compressed), decompressedOut);
        byte[] decompressed = decompressedOut.toByteArray();
        
        assertArrayEquals("Stream compression/decompression should round-trip", input, decompressed);
    }

    @Test
    public void testOdzOutputStream() throws IOException {
        byte[] input = "Test data for OdzOutputStream".getBytes();
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (OdzOutputStream odzOut = new OdzOutputStream(out)) {
            odzOut.write(input);
        }
        byte[] compressed = out.toByteArray();
        
        // Verify it's valid compressed data
        byte[] decompressed = Decompressor.decompressSimple(compressed);
        assertArrayEquals("OdzOutputStream should compress correctly", input, decompressed);
    }

    @Test
    public void testOdzInputStream() throws IOException {
        byte[] input = "Test data for OdzInputStream".getBytes();
        byte[] compressed = Compressor.compressSimple(input);
        
        try (OdzInputStream odzIn = new OdzInputStream(new ByteArrayInputStream(compressed))) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = odzIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            byte[] decompressed = out.toByteArray();
            assertArrayEquals("OdzInputStream should decompress correctly", input, decompressed);
        }
    }

    @Test
    public void testOdzInputStreamReadByte() throws IOException {
        byte[] input = "ABC".getBytes();
        byte[] compressed = Compressor.compressSimple(input);
        
        try (OdzInputStream odzIn = new OdzInputStream(new ByteArrayInputStream(compressed))) {
            assertEquals('A', odzIn.read());
            assertEquals('B', odzIn.read());
            assertEquals('C', odzIn.read());
            assertEquals(-1, odzIn.read()); // EOF
        }
    }

    @Test
    public void testOdzInputStreamAvailable() throws IOException {
        byte[] input = "Test".getBytes();
        byte[] compressed = Compressor.compressSimple(input);
        
        try (OdzInputStream odzIn = new OdzInputStream(new ByteArrayInputStream(compressed))) {
            // available() should return 0 before first read (lazy initialization)
            assertEquals(0, odzIn.available());
            
            // After first read, should return remaining bytes
            odzIn.read();
            assertEquals(3, odzIn.available());
        }
    }

    @Test
    public void testStreamingLargeData() throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append("test");
        }
        byte[] input = sb.toString().getBytes();
        
        ByteArrayOutputStream compressedOut = new ByteArrayOutputStream();
        Compressor.compress(new ByteArrayInputStream(input), compressedOut);
        byte[] compressed = compressedOut.toByteArray();
        
        ByteArrayOutputStream decompressedOut = new ByteArrayOutputStream();
        Decompressor.decompress(new ByteArrayInputStream(compressed), decompressedOut);
        byte[] decompressed = decompressedOut.toByteArray();
        
        assertArrayEquals("Large data should round-trip via streams", input, decompressed);
    }

    @Test
    public void testOdzOutputStreamChunkedWrite() throws IOException {
        byte[] input = "Chunked write test".getBytes();
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (OdzOutputStream odzOut = new OdzOutputStream(out)) {
            // Write in chunks
            odzOut.write(input, 0, 7);
            odzOut.write(input, 7, 6);
            odzOut.write(input, 13, input.length - 13);
        }
        byte[] compressed = out.toByteArray();
        
        byte[] decompressed = Decompressor.decompressSimple(compressed);
        assertArrayEquals("Chunked writes should work correctly", input, decompressed);
    }

    @Test
    public void testOdzInputStreamPartialRead() throws IOException {
        byte[] input = "Partial read test data".getBytes();
        byte[] compressed = Compressor.compressSimple(input);
        
        try (OdzInputStream odzIn = new OdzInputStream(new ByteArrayInputStream(compressed))) {
            byte[] buffer = new byte[5];
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            
            int bytesRead;
            while ((bytesRead = odzIn.read(buffer)) != -1) {
                result.write(buffer, 0, bytesRead);
            }
            
            assertArrayEquals("Partial reads should work correctly", input, result.toByteArray());
        }
    }
}
