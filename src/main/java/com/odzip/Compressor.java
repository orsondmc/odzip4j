package com.odzip;

import java.util.ArrayList;
import java.util.List;

/**
 * Compressor for ODZ format
 */
public final class Compressor {
    
    private Compressor() {
        // Utility class
    }
    
    public static byte[] compressSimple(byte[] in) {
        int n = in.length;
        List<Byte> outList = new ArrayList<>(n + n / 7 + 32);
        
        // Write magic header
        outList.add((byte)'O');
        outList.add((byte)'D');
        outList.add((byte)'Z');
        outList.add(OdzConstants.ODZ_VERSION);
        
        // Write raw size (4 bytes, little-endian)
        byte[] sizeBytes = new byte[4];
        OdzUtil.writeU32LE(sizeBytes, 0, n);
        for (byte b : sizeBytes) {
            outList.add(b);
        }
        
        // Initialize matcher
        LzMatcher matcher = new LzMatcher(n, OdzConstants.HASH_BITS, OdzConstants.MAX_CHAIN_STEPS);
        
        int i = 0;
        while (i < n) {
            int flagPos = outList.size();
            outList.add((byte)0); // placeholder for flags
            byte flags = 0;
            
            for (int k = 0; k < 8 && i < n; k++) {
                int[] bestLen = new int[1];
                int[] bestDist = new int[1];
                
                // Find best match
                matcher.findBest(in, i, n, OdzConstants.ODZ_WINDOW, 
                               OdzConstants.ODZ_MIN_MATCH, OdzConstants.ODZ_MAX_MATCH,
                               bestLen, bestDist);
                
                // Lazy matching (optional, cheap)
                if (bestLen[0] == OdzConstants.ODZ_MIN_MATCH && i + 1 < n) {
                    // Insert i before peeking next
                    matcher.insert(in, i);
                    int[] nextLen = new int[1];
                    int[] nextDist = new int[1];
                    matcher.findBestNext(in, i, n, OdzConstants.ODZ_WINDOW,
                                       OdzConstants.ODZ_MIN_MATCH, OdzConstants.ODZ_MAX_MATCH,
                                       nextLen, nextDist);
                    if (nextLen[0] > bestLen[0]) {
                        // Prefer longer future match
                        outList.add(in[i++]);
                        continue; // token k done (literal), skip to next k
                    }
                }
                
                if (bestLen[0] >= OdzConstants.ODZ_MIN_MATCH) {
                    flags |= (1 << k);
                    outList.add((byte)(bestLen[0] - OdzConstants.ODZ_MIN_MATCH));
                    outList.add((byte)(bestDist[0] & 0xFF));
                    outList.add((byte)(bestDist[0] >> 8));
                    
                    // Insert positions covered by the match
                    int end = i + bestLen[0];
                    matcher.insert(in, i);
                    if (i + 1 < end) {
                        matcher.insert(in, i + 1);
                    }
                    if (i + 2 < end) {
                        matcher.insert(in, i + 2);
                    }
                    
                    i += bestLen[0];
                } else {
                    // No match → literal
                    matcher.insert(in, i);
                    outList.add(in[i++]);
                }
            }
            
            // Update flags byte
            outList.set(flagPos, flags);
        }
        
        // Convert List<Byte> to byte[]
        byte[] result = new byte[outList.size()];
        for (int j = 0; j < outList.size(); j++) {
            result[j] = outList.get(j);
        }
        
        return result;
    }
}
