# ODZip4J

Minimal file compression library for Java.

Archives & encryption coming soon.

## Building

Build with Maven:

```sh
mvn clean package
```

This will create `target/odzip4j-1.0.0.jar` with the main class configured.

## Usage

### Command Line

```sh
# Compress
java -jar target/odzip4j-1.0.0.jar c input.txt output.odz

# Decompress
java -jar target/odzip4j-1.0.0.jar d output.odz output.txt
```

### As a Library

```java
import com.odzip.Compressor;
import com.odzip.Decompressor;

// Compress
byte[] compressed = Compressor.compressSimple(inputBytes);

// Decompress
byte[] decompressed = Decompressor.decompressSimple(compressed);
```

## Format

The ODZ format is:
- Header: `"ODZ\VERSION"` (4 bytes)
- Raw size: 32-bit little-endian integer (4 bytes)
- Groups: Each group contains a flags byte followed by up to 8 tokens (LSB-first):
  - Bit=0 → literal: 1 byte
  - Bit=1 → match: 3 bytes `[len_minus_MIN_MATCH][dist_low][dist_high]`

## Requirements

- Java 11 or higher
- Maven 3.6+ (for building)
- No external dependencies (pure JDK)

## Disclaimer

This project is in early alpha:
- It WILL overwrite files if given an output that already exists
- It currently doesn't preserve file permissions/inode data

PRs/issues are welcome.
