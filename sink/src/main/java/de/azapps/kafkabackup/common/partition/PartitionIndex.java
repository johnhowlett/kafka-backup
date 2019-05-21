package de.azapps.kafkabackup.common.partition;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PartitionIndex {
    private List<PartitionIndexEntry> index = new ArrayList<>();
    private FileOutputStream fileOutputStream;
    private FileInputStream fileInputStream;
    private long latestStartOffset = -1;

    PartitionIndex(Path indexFile) throws IOException, IndexException {
        this.fileInputStream = new FileInputStream(indexFile.toFile());
        this.fileOutputStream = new FileOutputStream(indexFile.toFile(), true);
        fileInputStream.getChannel().position(0);
        while (true) {
            try {
                PartitionIndexEntry partitionIndexEntry = PartitionIndexEntry.fromStream(fileInputStream);
                if (partitionIndexEntry.startOffset() <= latestStartOffset) {
                    throw new IndexException("Offsets must be always increasing! There is something terribly wrong in your index!");
                }
                index.add(partitionIndexEntry);
                latestStartOffset = partitionIndexEntry.startOffset();
            } catch (EOFException e) {
                // reached End of File
                break;
            }
        }
    }

    void nextSegment(String segmentFile, long startOffset) throws IOException {
        PartitionIndexEntry indexEntry = new PartitionIndexEntry(fileOutputStream, segmentFile, startOffset);
        index.add(indexEntry);
    }

    void close() throws IOException {
        fileInputStream.close();
        fileOutputStream.close();
    }

    void flush() throws IOException {
        fileOutputStream.flush();
    }

    public static class IndexException extends Exception {
        IndexException(String message) {
            super(message);
        }
    }

    long latestStartOffset() {
        return latestStartOffset;
    }
}