package com.ksig.vcs_cli.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

public class TokenStorageModule {

    private static final String VCS_DIR = ".tu_vcs";
    private static final String CREDENTIALS_FILE = "credentials.json";
    private final ObjectMapper objectMapper;

    public TokenStorageModule() {
        this.objectMapper = new ObjectMapper();
    }

    private Path getCredentialsFilePath() throws IOException {
        String userHome = System.getProperty("user.home");
        Path vcsDirPath = Paths.get(userHome, VCS_DIR);

        if (!Files.exists(vcsDirPath)) {
            Files.createDirectories(vcsDirPath);
        }
        return vcsDirPath.resolve(CREDENTIALS_FILE);
    }

    public void saveTokens(String accessToken, String refreshToken) {
        try {
            Path filePath = getCredentialsFilePath();

            ObjectNode tokenData = objectMapper.createObjectNode();
            tokenData.put("access_token", accessToken);
            tokenData.put("refresh_token", refreshToken);

            Files.writeString(filePath, tokenData.toPrettyString());
        } catch (IOException e) {
            System.err.println("Failed to store credentials: " + e.getMessage());
        }
    }

    public String getAccessToken() {
        try {
            Path credentialsPath = Path.of(System.getProperty("user.home"), ".tu_vcs", "credentials.json");

            if (!Files.exists(credentialsPath)) {
                return null;
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(Files.readString(credentialsPath));

            return root.path("access_token").asText(null);

        } catch (Exception e) {
            return null;
        }
    }

    public String getRefreshToken() {
        try {
            Path credentialsPath = Path.of(System.getProperty("user.home"), ".tu_vcs", "credentials.json");

            if (!Files.exists(credentialsPath)) {
                return null;
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(Files.readString(credentialsPath));

            return root.path("refresh_token").asText(null);

        } catch (Exception e) {
            return null;
        }
    }

    public void clearTokens() {
        try {
            Files.deleteIfExists(getCredentialsFilePath());
        } catch (IOException e) {
            System.err.println("Failed to clear credentials.");
        }
    }
}