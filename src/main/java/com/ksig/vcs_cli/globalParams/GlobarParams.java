package com.ksig.vcs_cli.globalParams;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class GlobarParams {
    public static final String KEYCLOACK_CLIENT_ID = "vcs-spring-client";
    public static final String KEYCLOAK_TOKEN_URL = "http://localhost:8081/realms/vcs-realm/protocol/openid-connect/token";
    public static final String REPO_META_DIR = ".tu_vcs_repo";
    public static final String REPO_META_FILE_NAME = "repo.json";
    public static final String ITEMS_META_FILE_NAME = "items.json";
}
