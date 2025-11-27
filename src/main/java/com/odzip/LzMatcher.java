package com.odzip;

/**
 * LZ77 matcher using hash chains for finding matches
 */
public class LzMatcher {
    private int[] head;
    private int[] prev;
    private int n;
    private int hashMask;
    private int maxChainSteps;
    
    public LzMatcher(int nBlock, int hashBits, int maxChainSteps) {
        int hashSize = 1 << hashBits;
        this.head = new int[hashSize];
        this.prev = new int[nBlock];
        this.n = nBlock;
        this.hashMask = hashSize - 1;
        this.maxChainSteps = maxChainSteps;
        
        // Initialize head array with -1
        for (int i = 0; i < head.length; i++) {
            head[i] = -1;
        }
    }
    
    public void reset(int nBlock) {
        this.n = nBlock;
        // Reset head array with -1
        for (int i = 0; i < head.length; i++) {
            head[i] = -1;
        }
    }
    
    private static int hash3(byte a, byte b, byte c, int mask) {
        int k = ((a & 0xFF) << 16) ^ ((b & 0xFF) << 8) ^ (c & 0xFF);
        return (int)((k * 2654435761L) & mask);
    }
    
    public void insert(byte[] in, int i) {
        if (i + 2 >= n) {
            prev[i] = -1;
            return;
        }
        int h = hash3(in[i], in[i + 1], in[i + 2], hashMask);
        prev[i] = head[h];
        head[h] = i;
    }
    
    private static int matchLen(byte[] a, int aOff, byte[] b, int bOff, int maxLen) {
        int len = 0;
        // Simple byte-by-byte comparison
        while (len < maxLen && a[aOff + len] == b[bOff + len]) {
            len++;
        }
        return len;
    }
    
    public void findBest(byte[] in, int i, int n, int window, int minMatch, int maxMatch,
                        int[] outLen, int[] outDist) {
        int bestLen = 0;
        int bestDist = 0;
        
        if (i + minMatch <= n) {
            int h = hash3(in[i], in[i + 1], in[i + 2], hashMask);
            int p = head[h];
            int steps = 0;
            int maxl = Math.min(n - i, maxMatch);
            
            while (p >= 0 && steps++ < maxChainSteps) {
                int dist = i - p;
                if (dist > 0 && dist <= window) {
                    int l = matchLen(in, p, in, i, maxl);
                    if (l >= minMatch && (l > bestLen || (l == bestLen && dist < bestDist))) {
                        bestLen = l;
                        bestDist = dist;
                        if (l == maxl) {
                            break; // best possible at this i
                        }
                    }
                }
                p = prev[p];
            }
        }
        
        outLen[0] = bestLen;
        outDist[0] = bestDist;
    }
    
    public void findBestNext(byte[] in, int i, int n, int window, int minMatch, int maxMatch,
                            int[] outLen, int[] outDist) {
        if (i + 1 >= n) {
            outLen[0] = 0;
            outDist[0] = 0;
            return;
        }
        // Pretend i+1 is the current position
        findBest(in, i + 1, n, window, minMatch, maxMatch, outLen, outDist);
    }
}
