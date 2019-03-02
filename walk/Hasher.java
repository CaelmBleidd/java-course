package ru.ifmo.rain.menshutin.walk;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

class Hasher extends SimpleFileVisitor<Path> {
    private final static int FNV_32_PRIME = 0x01000193;
    private final static int START = 0x811c9dc5;
    private final static int BUFFER = 4096;
    private byte[] bufferData = new byte[BUFFER];


    private BufferedWriter writer;


    Hasher(BufferedWriter writer) {
        this.writer = writer;
    }

    private void writeToFile(int hash, Path path) {
        String toWrite = String.format("%08x", hash).toLowerCase() + " " + path;
        try {
            writer.write(toWrite);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Can't write to the file: " + e.getMessage());
        }
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        int hash = START;
        try (var reader = new FileInputStream(file.toString())) {
            try {
                int cntReader;
                while ((cntReader = reader.read(bufferData, 0, BUFFER)) >= 0) {
                    for (int i = 0; i < cntReader; i++) {
                        hash = (hash * FNV_32_PRIME) ^ (bufferData[i] & 0xff);
                    }
                }
            } catch (IOException e) {
                System.err.println("Problem while hashing file " + file);
                hash = 0;
            }
        } catch (FileNotFoundException e) {
            System.err.println("Can't find file " + file);
            hash = 0;
        } catch (SecurityException e) {
            System.err.println("Security exception in file " + file);
            hash = 0;
        } catch (IOException e) {
            System.err.println("IOException while reading file " + file);
            hash = 0;
        } finally {
            writeToFile(hash, file);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        writeToFile(0, file);
        System.err.println("Problem while reading file \"" + file + "\".");
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        if (exc == null)
            return FileVisitResult.CONTINUE;
        else {
            System.err.println("There was a problem in walking on directory \"" + dir + "\".");
            return FileVisitResult.CONTINUE;
        }
    }
}
