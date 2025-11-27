package com.odzip;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Main entry point for ODZ compression/decompression tool
 */
public class Main {
    
    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("usage:");
            System.err.println("  java -jar odzip4j.jar c <in> <out>");
            System.err.println("  java -jar odzip4j.jar d <in> <out>");
            System.exit(2);
        }
        
        char mode = args[0].charAt(0);
        String inputPath = args[1];
        String outputPath = args[2];
        
        try {
            byte[] input = Files.readAllBytes(Paths.get(inputPath));
            byte[] output;
            
            if (mode == 'c') {
                output = Compressor.compressSimple(input);
            } else if (mode == 'd') {
                output = Decompressor.decompressSimple(input);
            } else {
                OdzUtil.die("mode must be c or d");
                return; // unreachable
            }
            
            Files.write(Paths.get(outputPath), output);
        } catch (IOException e) {
            OdzUtil.die("IO error: " + e.getMessage());
        }
    }
}
