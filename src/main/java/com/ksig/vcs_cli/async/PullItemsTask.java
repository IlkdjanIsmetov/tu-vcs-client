package com.ksig.vcs_cli.async;

import com.ksig.vcs_cli.http.BackendRestClient;
import com.ksig.vcs_cli.models.SyncItemView;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.net.URIBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PullItemsTask implements Runnable {
    private final SyncItemView itemToSync;
    private final BackendRestClient backendRestClient;
    private final URIBuilder baseURL;
    public  PullItemsTask(SyncItemView itemToSync, BackendRestClient backendRestClient, URIBuilder baseURL) {
        this.itemToSync = itemToSync;
        this.backendRestClient = backendRestClient;
        this.baseURL = baseURL;
    }

    @Override
    public void run() {
        switch (itemToSync.getStatus()) {
            case DELETED_REMOTE: deleteLocalFile(itemToSync); break;
            case NEW_REMOTE: downloadRemoteFile(itemToSync);
            case MODIFIED_REMOTE: handleModifiedRemoteFile(itemToSync); break;
        }
    }

    private void deleteLocalFile(SyncItemView itemToSync) {
        try {
            Files.delete(itemToSync.getLocalPath());
        } catch (IOException e) {
            System.err.println("Error deleting local file " + itemToSync.getPath());
        }
        System.out.println("Deleted local file " + itemToSync.getPath());
    }

    private void downloadRemoteFile(SyncItemView itemToSync) {
        try {
            URI uri = baseURL.appendPath("content").appendPath(itemToSync.getStorageKey()).build();
            HttpGet httpGet = new HttpGet(uri);
            backendRestClient.downloadFile(httpGet, itemToSync.getLocalPath());
        } catch (Exception e) {
            System.err.println("Error downloading remote file " + itemToSync.getPath());
        }
        System.out.println("Downloaded remote file " + itemToSync.getPath());
    }

    private void handleModifiedRemoteFile(SyncItemView itemToSync) {
        try {
            Files.delete(itemToSync.getLocalPath());
            URI uri = baseURL.appendPath("content").appendPath(itemToSync.getStorageKey()).build();
            HttpGet httpGet = new HttpGet(uri);
            backendRestClient.downloadFile(httpGet, itemToSync.getLocalPath());
        } catch (Exception e) {
            System.err.println("Error handling remote file " + itemToSync.getPath());
        }
        System.out.println("Changed remote modified file " + itemToSync.getPath());
    }
}
