package com.odzip;

import java.io.IOException;
import java.io.InputStream;

/**
 * InputStream that decompresses ODZ format data from an underlying InputStream
 */
public class OdzInputStream extends InputStream {
    private final InputStream in;
    private byte[] decompressedBuffer;
    private int decompressedPosition;
    private int decompressedSize;
    
    // Compressed data buffer
    private byte[] compressedBuffer;
    private int compressedPosition;
    private int compressedSize;
    private static final int COMPRESSED_BUFFER_SIZE = 8192;
    
    // Decompression state
    private int rawLen;
    private int decompressedOp;
    private boolean headerRead;
    private boolean decompressionComplete;
    
    public OdzInputStream(InputStream in) {
        this.in = in;
        this.decompressedPosition = 0;
        this.decompressedSize = 0;
        this.compressedBuffer = new byte[COMPRESSED_BUFFER_SIZE];
        this.compressedPosition = 0;
        this.compressedSize = 0;
        this.decompressedOp = 0;
        this.headerRead = false;
        this.decompressionComplete = false;
    }
    
    private void readHeader() throws IOException {
        if (headerRead) {
            return;
        }
        
        byte[] header = new byte[8];
        int totalRead = 0;
        while (totalRead < 8) {
            int bytesRead = in.read(header, totalRead, 8 - totalRead);
            if (bytesRead == -1) {
                OdzUtil.die("truncated header");
            }
            totalRead += bytesRead;
        }
        
        // Check magic header
        if (header[0] != 'O' || header[1] != 'D' || header[2] != 'Z' || header[3] != OdzConstants.ODZ_VERSION) {
            OdzUtil.die("bad magic");
        }
        
        rawLen = OdzUtil.readU32LE(header, 4);
        decompressedBuffer = new byte[rawLen];
        headerRead = true;
    }
    
    private int readCompressedData(int minBytes) throws IOException {
        // Ensure we have enough data in the buffer
        while (compressedSize - compressedPosition < minBytes) {
            // Shift remaining data to the beginning
            if (compressedPosition > 0 && compressedPosition < compressedSize) {
                System.arraycopy(compressedBuffer, compressedPosition, compressedBuffer, 0, 
                               compressedSize - compressedPosition);
                compressedSize -= compressedPosition;
                compressedPosition = 0;
            }
            
            // Read more data
            int bytesRead = in.read(compressedBuffer, compressedSize, 
                                   compressedBuffer.length - compressedSize);
            if (bytesRead == -1) {
                break;
            }
            compressedSize += bytesRead;
        }
        
        return compressedSize - compressedPosition;
    }
    
    private byte readCompressedByte() throws IOException {
        if (compressedPosition >= compressedSize) {
            readCompressedData(1);
        }
        if (compressedPosition >= compressedSize) {
            OdzUtil.die("unexpected end of compressed data");
        }
        return compressedBuffer[compressedPosition++];
    }
    
    private void decompressIncremental(int targetSize) throws IOException {
        if (!headerRead) {
            readHeader();
        }
        
        if (decompressionComplete) {
            return;
        }
        
        // Read compressed data as needed
        while (decompressedOp < targetSize && decompressedOp < rawLen) {
            // Read flags byte
            if (compressedPosition >= compressedSize) {
                readCompressedData(1);
            }
            if (compressedPosition >= compressedSize) {
                OdzUtil.die("unexpected end of compressed data");
            }
            
            byte flags = compressedBuffer[compressedPosition++];
            
            for (int k = 0; k < 8 && decompressedOp < rawLen; k++) {
                if ((flags & (1 << k)) != 0) {
                    // Match token (3 bytes)
                    if (compressedSize - compressedPosition < 3) {
                        readCompressedData(3);
                    }
                    if (compressedSize - compressedPosition < 3) {
                        OdzUtil.die("corrupt match token");
                    }
                    
                    int len = (compressedBuffer[compressedPosition++] & 0xFF) + OdzConstants.ODZ_MIN_MATCH;
                    int dist = (compressedBuffer[compressedPosition++] & 0xFF) | 
                              ((compressedBuffer[compressedPosition++] & 0xFF) << 8);
                    
                    if (dist <= 0 || dist > decompressedOp) {
                        OdzUtil.die("bad distance");
                    }
                    
                    int from = decompressedOp - dist;
                    for (int t = 0; t < len && decompressedOp < rawLen; t++) {
                        decompressedBuffer[decompressedOp++] = decompressedBuffer[from + t];
                    }
                } else {
                    // Literal token (1 byte)
                    if (compressedPosition >= compressedSize) {
                        readCompressedData(1);
                    }
                    if (compressedPosition >= compressedSize) {
                        OdzUtil.die("corrupt literal");
                    }
                    decompressedBuffer[decompressedOp++] = compressedBuffer[compressedPosition++];
                }
            }
        }
        
        decompressedSize = decompressedOp;
        
        if (decompressedOp >= rawLen) {
            decompressionComplete = true;
            if (decompressedOp != rawLen) {
                OdzUtil.die("size mismatch");
            }
        }
    }
    
    @Override
    public int read() throws IOException {
        if (decompressedPosition >= decompressedSize) {
            // Try to decompress more data
            decompressIncremental(decompressedPosition + 1);
            if (decompressedPosition >= decompressedSize) {
                return -1;
            }
        }
        return decompressedBuffer[decompressedPosition++] & 0xFF;
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        
        // Ensure we have enough decompressed data
        decompressIncremental(decompressedPosition + len);
        
        if (decompressedPosition >= decompressedSize) {
            return -1;
        }
        
        int available = decompressedSize - decompressedPosition;
        int toRead = Math.min(len, available);
        System.arraycopy(decompressedBuffer, decompressedPosition, b, off, toRead);
        decompressedPosition += toRead;
        return toRead;
    }
    
    @Override
    public int available() throws IOException {
        if (!headerRead) {
            return 0;
        }
        
        // Try to decompress more data if possible
        if (!decompressionComplete) {
            try {
                decompressIncremental(decompressedPosition + 1);
            } catch (IOException e) {
                // If we can't read more, return what we have
            }
        }
        
        return decompressedSize - decompressedPosition;
    }
    
    @Override
    public void close() throws IOException {
        in.close();
    }
}
