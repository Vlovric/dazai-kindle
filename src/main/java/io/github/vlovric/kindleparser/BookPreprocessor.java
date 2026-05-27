package io.github.vlovric.kindleparser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BookPreprocessor {

    /**
     * Checks if the input file is .azw3. If so, invokes Calibre's ebook-convert
     * to convert it to .epub in the same directory (or a temp directory) and returns
     * the path to the new .epub file. Otherwise, returns the original path.
     */
    public static Path preprocess(Path inputFile) throws IOException, InterruptedException {
        String filename = inputFile.getFileName().toString();
        
        if (filename.toLowerCase().endsWith(".azw3")) {
            System.out.println("Processing AZW3 file. Converting to EPUB using Calibre...");
            
            String epubFilename = filename.substring(0, filename.lastIndexOf('.')) + ".epub";
            Path epubPath = inputFile.resolveSibling(epubFilename);
            
            if (Files.exists(epubPath)) {
                System.out.println("EPUB version already exists. Using " + epubPath);
                return epubPath;
            }

            ProcessBuilder pb = new ProcessBuilder(
                "ebook-convert",
                inputFile.toAbsolutePath().toString(),
                epubPath.toAbsolutePath().toString()
            );
            
            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                throw new IOException("ebook-convert failed with exit code " + exitCode + ". Is Calibre installed?");
            }
            
            return epubPath;
        }

        return inputFile;
    }
}
