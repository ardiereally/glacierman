package com.rdas.cli;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.rdas.common.ArchiveInfo;
import com.rdas.common.Credentials;
import com.rdas.glacier.GlacierDownload;
import com.rdas.glacier.GlacierInventory;
import com.rdas.glacier.GlacierUpload;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Entrypoint {

    private String action;

    private String vaultName;

    private File uploadArchive;

    private File downloadRequestsFile;

    private Credentials credentials;

    private DownloadArchiveRequest request;

    public static void main(final String[] args) throws IOException, InterruptedException {
        new Entrypoint().execute(args);
    }

    public void execute(final String[] args) throws IOException, InterruptedException {
        parseArgs(args);
        loadCredentials();
        switch (action) {
            case "upload":
                doUpload();
                break;
            case "download":
                loadDownloadRequest();
                doDownload();
                break;
            case "inventory":
                doInventory();
                break;
            default:
        }
    }

    private void doUpload() {
        System.out.println("Starting upload...");
        final ArchiveInfo archiveInfo = ArchiveInfo.ofLocal(vaultName, uploadArchive);

        System.out.println("Will upload \"" + uploadArchive.getName() + "\" to vault \"" + vaultName + "\" in " + credentials.getRegion());

        final double sizeMb = uploadArchive.length() / (1024.0 * 1024);
        final GlacierUpload glUpload = new GlacierUpload(archiveInfo, credentials);
        System.out.println("Initialized. Archive size is: " + sizeMb + " MB");

        final long start = System.currentTimeMillis();
        final String archiveId = glUpload.upload();
        System.out.println("Archive ID is " + archiveId);
        reportSpeed(sizeMb, start);
    }

    private void doDownload() throws InterruptedException {
        System.out.println("Starting download...");
        final ArchiveInfo archiveInfo = ArchiveInfo.ofRemote(vaultName, request.getArchiveId(), new File(request.getLocalFileName()), request.getFileSize());

        System.out.println("Will download archive with id \"" + archiveInfo.getRemoteArchiveId() + "\" from vault \"" + vaultName + "\" to local file \"" + archiveInfo.getLocalArchiveFile() + "\"");

        final GlacierDownload glacierDownload = new GlacierDownload(archiveInfo, credentials);
        final String jobId = glacierDownload.prepareArchive();
        final long start = System.currentTimeMillis();
        glacierDownload.download(jobId);

        final double sizeMb = archiveInfo.getLocalArchiveFile().length() / (1024.0 * 1024);
        System.out.println("Downloaded archive size is: " + sizeMb + " MB");
        reportSpeed(sizeMb, start);
    }

    private void doInventory() throws IOException, InterruptedException {
        System.out.println("Starting inventory of " + vaultName + " ...");
        final GlacierInventory inventory = new GlacierInventory(credentials);
        inventory.inventory(vaultName);
    }

    private void parseArgs(final String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Need at least 2 arguments");
        }

        final String action = args[0];
        switch (action) {
            case "upload":
            case "download":
            case "inventory":
                this.action = action;
                break;
            default:
                throw new IllegalArgumentException("action must be 'upload' or 'download' or 'inventory'");
        }

        final String vaultName = args[1];
        if (vaultName.equals("")) {
            throw new IllegalArgumentException("Vault name must be provided");
        } else {
            this.vaultName = vaultName;
        }

        if (this.action.equals("upload") || this.action.equals("download")) {
            if (args.length < 3) {
                throw new IllegalArgumentException("Need 3 arguments");
            }
            final File argFile = new File(args[2]);
            if (!(argFile.exists() && argFile.isFile())) {
                throw new IllegalArgumentException("Argument file must exist & be a file");
            }

            if (this.action.equals("upload")) {
                this.uploadArchive = argFile;
            } else {
                this.downloadRequestsFile = argFile;
            }
        }
    }

    private void loadCredentials() throws FileNotFoundException {
        final Gson gson = new Gson();
        final JsonReader reader = new JsonReader(new FileReader("credentials.json"));
        this.credentials = gson.fromJson(reader, Credentials.class);
    }

    private void loadDownloadRequest() throws FileNotFoundException {
        final Gson gson = new Gson();
        final JsonReader reader = new JsonReader(new FileReader(downloadRequestsFile));
        this.request = gson.fromJson(reader, DownloadArchiveRequest.class);
    }

    private void reportSpeed(final double sizeMb, final long start) {
        final double duration = (System.currentTimeMillis() - start) / 1000.0;
        final double speed = sizeMb / duration;
        System.out.println("Transferred " + sizeMb + " MBs in " + duration + " seconds at an average speed of " + speed + " MB/s");
        System.out.println("Exiting...");
    }
}
