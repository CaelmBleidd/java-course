package ru.ifmo.rain.menshutin.walk;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;

public class RecursiveWalk {

    public static void main(String[] args) {
        try {
            if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
                System.err.println("Two arguments expected: <inputFile> <outputFile>");
                return;
            }

            Path inputFilePath;
            try {
                inputFilePath = Paths.get(args[0]);
            } catch (InvalidPathException e) {
                printError("Invalid path to the input file", e);
                return;
            }

            Path outputFilePath;
            try {
                outputFilePath = Paths.get(args[1]);
            } catch (InvalidPathException e) {
                printError("Invalid path to the output file", e);
                return;
            }



            try (var reader = Files.newBufferedReader(inputFilePath)) {
                try (var writer = Files.newBufferedWriter(outputFilePath)) {
                    for (var pathName = reader.readLine(); pathName != null; pathName = reader.readLine()) {
                        try {
                            var path = Paths.get(pathName);
                            Files.walkFileTree(path, new Hasher(writer));
                        } catch (InvalidPathException e) {
                            writer.write(String.format("%08X", 0).toLowerCase() + " " + pathName);
                            printError("Invalid path: " + pathName, e);
                        } catch (SecurityException e) {
                            writer.write(String.format("%08X", 0).toLowerCase() + " " + pathName);
                            printError("Security exception in file " + pathName, e);
                        } catch (IOException e) {
                            printError("IOException while reading input file", e);
                        }
                    }
                } catch (FileNotFoundException e) {
                    printError("Output file not found", e);
                } catch (SecurityException e) {
                    printError("Security exception during opening output file", e);
                } catch (IOException e) {
                    printError("IO exception with output file.", e);
                }

            } catch (FileNotFoundException e) {
                printError("Input file not found", e);
            } catch (SecurityException e) {
                printError("Security exception during opening input file", e);
            } catch (IOException e) {
                printError("IO exception with input file.", e);
            }
        } catch (RuntimeException e) {
            printError("Caught runtime exception", e);
        }

    }

    private static void printError(String message, Exception e) {
        System.err.println(message);
        System.err.println("Exception message: " + e.getMessage());
    }

}
