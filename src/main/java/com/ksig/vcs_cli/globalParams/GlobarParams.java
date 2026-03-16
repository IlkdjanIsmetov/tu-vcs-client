package com.ksig.vcs_cli.globalParams;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class GlobarParams {
    public static  String KEYCLOACK_CLIENT_ID;
    public static  String KEYCLOAK_TOKEN_URL;
    public static  String APP_BASE_URL;

    static {
        Properties configuration = new Properties();
        String fileName = "config.properties";
        try (InputStream input = GlobarParams.class.getClassLoader().getResourceAsStream(fileName)) {

            if (input == null) {
                System.err.println("Warning: Unable to find " + fileName + " in resources.");
            }
            configuration.load(input);
            KEYCLOACK_CLIENT_ID = configuration.getProperty("keycloackClientId");
            KEYCLOAK_TOKEN_URL = configuration.getProperty("keycloackTokenUrl");
            APP_BASE_URL = configuration.getProperty("appBaseUrl");

        } catch (IOException ex) {
            System.err.println("Error reading configuration file: " + ex.getMessage());
        }
    }
}
