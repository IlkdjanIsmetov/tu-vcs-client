package com.ksig.vcs_cli.async;

import com.ksig.vcs_cli.http.BackendRestClient;
import com.ksig.vcs_cli.models.SyncItemView;
import com.ksig.vcs_cli.models.enums.ItemType;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.net.URIBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

public class PullItemsTask implements Callable<Void> {
    private final SyncItemView itemToSync;
    private final BackendRestClient backendRestClient;
    private final URI baseURL;

    public PullItemsTask(SyncItemView itemToSync, BackendRestClient backendRestClient, URI baseURL) {
        this.itemToSync = itemToSync;
        this.backendRestClient = backendRestClient;
        this.baseURL = baseURL;
    }

    @Override
    public Void call() {
        switch (itemToSync.getStatus()) {
            case DELETED_REMOTE: deleteLocalFile(itemToSync); break;
            case NEW_REMOTE: downloadRemoteFile(itemToSync); break;
            case MODIFIED_REMOTE: handleModifiedRemoteFile(itemToSync); break;
        }
        return null;
    }

    private void deleteLocalFile(SyncItemView itemToSync) {
        try {
            Files.delete(itemToSync.getLocalPath());
            System.out.println("Deleted local " + itemToSync.getPath());
        } catch (IOException e) {
            System.err.println("Error deleting local file " + itemToSync.getPath() + " Local path: " + itemToSync.getLocalPath() );
            e.printStackTrace();
        }
    }

    private void downloadRemoteFile(SyncItemView itemToSync) {
        try {
            if (itemToSync.getItemType() == ItemType.DIRECTORY) {
                Files.createDirectories(itemToSync.getLocalPath());
                System.out.println("Created local directory " + itemToSync.getPath());
                return;
            }

            URI uri = new URIBuilder(baseURL).appendPath("content").appendPath(itemToSync.getStorageKey()).build();
            HttpGet httpGet = new HttpGet(uri);
            backendRestClient.downloadFileWithFileName(httpGet, itemToSync.getLocalPath());
            System.out.println("Downloaded remote file " + itemToSync.getPath());
        } catch (Exception e) {
            System.err.println("Error downloading remote file " + itemToSync.getPath() + " Local path: " + itemToSync.getLocalPath() );
            e.printStackTrace();
        }
    }

    private void handleModifiedRemoteFile(SyncItemView itemToSync) {
        try {
            if (itemToSync.getItemType() == ItemType.DIRECTORY) {
                System.out.println("Ignored modified status for directory " + itemToSync.getPath());
                return;
            }

            Files.delete(itemToSync.getLocalPath());
            URI uri = new URIBuilder(baseURL).appendPath("content").appendPath(itemToSync.getStorageKey()).build();
            HttpGet httpGet = new HttpGet(uri);
            backendRestClient.downloadFileWithFileName(httpGet, itemToSync.getLocalPath());
            System.out.println("Changed remote modified file " + itemToSync.getPath());
        } catch (Exception e) {
            System.err.println("Error handling remote file " + itemToSync.getPath() + " Local path: " + itemToSync.getLocalPath() );
            e.printStackTrace();
        }
    }
}