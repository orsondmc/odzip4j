package com.odzip;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Decompressor for ODZ format
 */
public final class Decompressor {
    
    private Decompressor() {
        // Utility class
    }
    
    public static byte[] decompressSimple(byte[] in) {
        if (in.length < 8) {
            OdzUtil.die("truncated");
        }
        
        // Check magic header
        if (in[0] != 'O' || in[1] != 'D' || in[2] != 'Z' || in[3] != OdzConstants.ODZ_VERSION) {
            OdzUtil.die("bad magic");
        }
        
        int rawLen = OdzUtil.readU32LE(in, 4);
        byte[] out = new byte[rawLen];
        
        int ip = 8;
        int op = 0;
        
        while (ip < in.length && op < rawLen) {
            byte flags = in[ip++];
            
            for (int k = 0; k < 8 && op < rawLen; k++) {
                if ((flags & (1 << k)) != 0) {
                    // Match token (3 bytes)
                    if (ip + 3 > in.length) {
                        OdzUtil.die("corrupt match token");
                    }
                    int len = (in[ip++] & 0xFF) + OdzConstants.ODZ_MIN_MATCH;
                    int dist = (in[ip++] & 0xFF) | ((in[ip++] & 0xFF) << 8);
                    
                    if (dist <= 0 || dist > op) {
                        OdzUtil.die("bad distance");
                    }
                    
                    int from = op - dist;
                    for (int t = 0; t < len; t++) {
                        if (op >= rawLen) {
                            OdzUtil.die("overrun");
                        }
                        out[op++] = out[from + t];
                    }
                } else {
                    // Literal token (1 byte)
                    if (ip >= in.length) {
                        OdzUtil.die("corrupt literal");
                    }
                    out[op++] = in[ip++];
                }
            }
        }
        
        if (op != rawLen) {
            OdzUtil.die("size mismatch");
        }
        
        return out;
    }
    
    /**
     * Decompress data from an InputStream and write to an OutputStream
     * 
     * @param in Input stream to read compressed data from
     * @param out Output stream to write decompressed data to
     * @throws IOException if an I/O error occurs
     */
    public static void decompress(InputStream in, OutputStream out) throws IOException {
        byte[] compressed = readAll(in);
        byte[] decompressed = decompressSimple(compressed);
        out.write(decompressed);
        out.flush();
    }
    
    private static byte[] readAll(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] temp = new byte[8192];
        int bytesRead;
        while ((bytesRead = in.read(temp)) != -1) {
            buffer.write(temp, 0, bytesRead);
        }
        return buffer.toByteArray();
    }
}
