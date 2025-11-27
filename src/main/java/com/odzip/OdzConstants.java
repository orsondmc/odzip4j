package com.odzip;

/**
 * Constants for ODZ compression format
 */
public final class OdzConstants {
    public static final byte ODZ_VERSION = 1;
    public static final int ODZ_WINDOW = 65535;
    public static final int ODZ_MIN_MATCH = 3;
    public static final int ODZ_MAX_MATCH = 258;
    public static final int HASH_BITS = 15;
    public static final int MAX_CHAIN_STEPS = 64;
    
    private OdzConstants() {
        // Utility class
    }
}
