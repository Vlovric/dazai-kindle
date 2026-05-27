package io.github.vlovric.kindleparser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Preprocesses eBook files by managing potential conversion requirements cleanly.
 * Serves as an interoperability shell to launch Calibre dependencies seamlessly.
 */
public class BookPreprocessor {

    /**
     * Checks if the input file is .azw3. If so, invokes Calibre's ebook-convert
     * to convert it to .epub in the same directory (or a temp directory) and returns
     * the path to the new .epub file. Otherwise, returns the original path.
     *
     * @param inputFile local filesystem path resolving original document
     * @return Path denoting standardized EPUB reference compatible with `EpubLoader`
     * @throws IOException when sub-process commands fail execution
     * @throws InterruptedException if current processing thread blocks
     */
    public static Path preprocess(Path inputFile) throws IOException, InterruptedException {
        String filename = inputFile.getFileName().toString();
        
        if (filename.toLowerCase().endsWith(".azw3")) {
            System.out.println("[KindleParser] Processing AZW3 file. Converting to EPUB using Calibre...");
            
            String epubFilename = filename.substring(0, filename.lastIndexOf('.')) + ".epub";
            Path epubPath = inputFile.resolveSibling(epubFilename);
            
            if (Files.exists(epubPath)) {
                System.out.println("[KindleParser] EPUB version already exists. Using " + epubPath);
                return epubPath;
            }

            ProcessBuilder pb = new ProcessBuilder(
                "ebook-convert",
                inputFile.toAbsolutePath().toString(),
                epubPath.toAbsolutePath().toString()
            );
            
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[Calibre] " + line);
                }
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                throw new IOException("ebook-convert failed with exit code " + exitCode + ". Is Calibre installed?");
            }
            
            return epubPath;
        }

        return inputFile;
    }
}
