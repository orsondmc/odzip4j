package com.odzip;

/**
 * Utility functions for ODZ format
 */
public final class OdzUtil {
    
    private OdzUtil() {
        // Utility class
    }
    
    public static void die(String message) {
        System.err.println("err: " + message);
        System.exit(1);
    }
    
    public static void writeU32LE(byte[] dst, int offset, int value) {
        dst[offset] = (byte)(value & 0xFF);
        dst[offset + 1] = (byte)((value >> 8) & 0xFF);
        dst[offset + 2] = (byte)((value >> 16) & 0xFF);
        dst[offset + 3] = (byte)((value >> 24) & 0xFF);
    }
    
    public static int readU32LE(byte[] src, int offset) {
        return (src[offset] & 0xFF) |
               ((src[offset + 1] & 0xFF) << 8) |
               ((src[offset + 2] & 0xFF) << 16) |
               ((src[offset + 3] & 0xFF) << 24);
    }
}
