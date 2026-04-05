package com.ksig.vcs_cli.commands;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import com.ksig.vcs_cli.http.BackendRestClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "clone", description = "Clone remote repository")
public class CloneCommand implements Callable<Integer> {
    @Option(names = {"-u", "--url"}, required = true, description = "Url to the remote repo")
    private String url;

    @Override
    public Integer call() {
        System.out.println("Cloning remote repository...");
        URI uri = null;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            System.err.println("Invalid url: " + url);
            return 1;
        }
        HttpGet httpGet = new HttpGet(uri);
        httpGet.setHeader("Accept", "application/octet-stream");
        BackendRestClient  backendRestClient = new BackendRestClient();
        Path downloadedFile;
        try {
            downloadedFile = backendRestClient.downloadFile(httpGet, Path.of(System.getProperty("user.dir")));
        } catch (Exception e) {
            System.err.println("Failed to clone remote repository!");
            System.err.println(e.getMessage());
            return 1;
        }
        Path unzipDir = Path.of(downloadedFile.toFile().getAbsolutePath().replaceAll(".zip", ""));
        try {
            extractZip(downloadedFile, unzipDir);
            Files.deleteIfExists(downloadedFile);
        } catch (IOException e) {
            System.err.println("Failed to download remote repository!");
            return 1;
        }
        System.out.println("Cloned remote repository successfully!");
        return 0;
    }

    private void extractZip(Path zipFilePath, Path targetDirectory) throws IOException {
        Files.createDirectories(targetDirectory);

        try (FileSystem zipFs = FileSystems.newFileSystem(zipFilePath)) {
            Path zipRoot = zipFs.getPath("/");

            try (Stream<Path> paths = Files.walk(zipRoot)) {
                paths.forEach(zipEntryPath -> {
                    try {
                        Path relativeZipPath = zipRoot.relativize(zipEntryPath);
                        Path destinationPath = targetDirectory.resolve(relativeZipPath.toString()).normalize();
                        if (Files.isDirectory(zipEntryPath)) {
                            Files.createDirectories(destinationPath);
                        } else {
                            Files.createDirectories(destinationPath.getParent());
                            Files.copy(zipEntryPath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                        }

                    } catch (IOException e) {
                        throw new UncheckedIOException("Failed to extract: " + zipEntryPath, e);
                    }
                });
            }
        }
    }
}
