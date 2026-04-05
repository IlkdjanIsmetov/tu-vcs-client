package com.ksig.vcs_cli.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksig.vcs_cli.exceptions.NotARepoException;
import com.ksig.vcs_cli.globalParams.GlobarParams;
import com.ksig.vcs_cli.models.ItemMeta;
import com.ksig.vcs_cli.models.RepositoryMeta;
import com.ksig.vcs_cli.models.enums.ItemType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class RepositoryStatus {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static class StatusResult {
        public final Path repoRoot;
        public final Map<String, Path> added = new HashMap<>();
        public final Map<String, Path> modified = new HashMap<>();
        public final Map<String, ItemMeta> deleted = new HashMap<>();
        public final long revision;
        public StatusResult(Path repoRoot, long revision) {
            this.repoRoot = repoRoot;
            this.revision = revision;
        }
    }

    public static StatusResult analyzeWorkspace() throws IOException, NotARepoException {
        Path repoRoot = findRepositoryRoot();
        RepositoryMeta repoMeta = getRepositoryMeta();
        StatusResult result = new StatusResult(repoRoot, repoMeta.getRevision());
        Path repoMetaDir = repoRoot.resolve(GlobarParams.REPO_META_DIR);
        Path itemsJsonPath = repoMetaDir.resolve(GlobarParams.ITEMS_META_FILE_NAME);

        List<ItemMeta> trackedList = mapper.readValue(
                itemsJsonPath.toFile(),
                new TypeReference<List<ItemMeta>>() {}
        );

        Map<String, ItemMeta> trackedItems = new HashMap<>();
        if (trackedList != null) {
            for (ItemMeta item : trackedList) {
                trackedItems.put(item.getPath(), item);
            }
        }
        if (trackedItems == null) {
            trackedItems = new HashMap<>();
        }

        Map<String, Path> currentFiles = new HashMap<>();
        try (Stream<Path> paths = Files.walk(repoRoot)) {
            paths.filter(path -> !path.equals(repoRoot))
                    .filter(path -> !path.startsWith(repoMetaDir))
                    .forEach(path -> {
                        String relativePath = repoRoot.relativize(path).toString().replace("\\", "/");
                        currentFiles.put(relativePath, path);
                    });
        }

        for (Map.Entry<String, Path> entry : currentFiles.entrySet()) {
            String relativePath = entry.getKey();
            Path path = entry.getValue();

            if (!trackedItems.containsKey(relativePath)) {
                result.added.put(relativePath, path);
            } else {
                ItemMeta meta = trackedItems.get(relativePath);
                if (meta.getItemType() == ItemType.FILE) {
                    String currentChecksum = calculateChecksum(path);
                    if (!currentChecksum.equals(meta.getChecksum())) {
                        result.modified.put(relativePath, path);
                    }
                }
                trackedItems.remove(relativePath);
            }
        }

        result.deleted.putAll(trackedItems);

        return result;
    }

    public static Path findRepositoryRoot() throws NotARepoException {
        Path checkPath = Path.of(System.getProperty("user.dir"));
        while (checkPath != null) {
            Path potentialRepoMeta = checkPath.resolve(GlobarParams.REPO_META_DIR);
            if (Files.isDirectory(potentialRepoMeta)) {
                return checkPath;
            }
            checkPath = checkPath.getParent();
        }
        throw new NotARepoException();
    }

    public static RepositoryMeta getRepositoryMeta() throws IOException, NotARepoException {
        Path repoRoot = findRepositoryRoot();
        Path repoMetaFile = repoRoot.resolve(GlobarParams.REPO_META_DIR).resolve(GlobarParams.REPO_META_FILE_NAME);
        return mapper.readValue(repoMetaFile.toFile(), RepositoryMeta.class);
    }

    public static String calculateChecksum(Path file) {
        if (Files.isDirectory(file)) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(file);
            byte[] hash = digest.digest(fileBytes);

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            System.err.println("Warning: Could not calculate checksum for " + file.toString());
            return "";
        }
    }
}