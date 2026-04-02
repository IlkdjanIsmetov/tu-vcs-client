package com.ksig.vcs_cli.commands;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
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
    public Integer call() throws Exception {
        URI uri = new URI(url);
        HttpGet httpGet = new HttpGet(uri);
        httpGet.setHeader("Accept", "application/octet-stream");
        BackendRestClient  backendRestClient = new BackendRestClient();
        Path downloadedFile = backendRestClient.downloadFile(httpGet, Path.of(System.getProperty("user.dir")));
        Path unzipDir = Path.of(downloadedFile.toFile().getAbsolutePath().replaceAll(".zip", ""));
        extractZip(downloadedFile, unzipDir);
        Files.deleteIfExists(downloadedFile);
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
