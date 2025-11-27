package com.odzip;

import java.io.IOException;
import java.io.OutputStream;

/**
 * OutputStream that compresses data to ODZ format and writes to an underlying OutputStream
 */
public class OdzOutputStream extends OutputStream {
    private final OutputStream out;
    private final java.io.ByteArrayOutputStream buffer;
    private boolean closed;
    
    public OdzOutputStream(OutputStream out) {
        this.out = out;
        this.buffer = new java.io.ByteArrayOutputStream();
        this.closed = false;
    }
    
    @Override
    public void write(int b) throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
        }
        buffer.write(b);
    }
    
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
        }
        buffer.write(b, off, len);
    }
    
    @Override
    public void flush() throws IOException {
        // Don't flush until close - compression needs all data
    }
    
    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        
        try {
            // Compress all buffered data
            byte[] input = buffer.toByteArray();
            byte[] compressed = Compressor.compressSimple(input);
            
            // Write compressed data to output stream
            out.write(compressed);
            out.flush();
        } finally {
            out.close();
        }
    }
}
