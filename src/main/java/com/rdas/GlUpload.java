package com.rdas;

import com.amazonaws.services.glacier.AmazonGlacier;
import com.amazonaws.services.glacier.AmazonGlacierClientBuilder;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManager;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManagerBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Uploader of archives to AWS Glacier
 */
public class GlUpload {
    private final String archivePath;
    private final String glacierVault;
    private final ArchiveTransferManager archiveTransferManager;

    public GlUpload(String archivePath, String glacierVault) {
        this.archivePath = archivePath;
        this.glacierVault = glacierVault;

        AmazonGlacier client = AmazonGlacierClientBuilder
                .standard()
                .build();
        this.archiveTransferManager = new ArchiveTransferManagerBuilder()
                .withGlacierClient(client)
                .build();
    }

    public static void main(String[] args) throws FileNotFoundException {
        System.out.println("Starting glupload...");

        String vaultName = Objects.requireNonNull(System.getProperty("vault"));
        String archivePath = Objects.requireNonNull(System.getProperty("archive"));

        if (Stream.of(vaultName, archivePath).anyMatch(v -> "".equals(v))) {
            System.err.println("Required argument not supplied. Need all of: vault, archive");
            System.exit(1);
        }

        System.out.println("Will upload " + archivePath + " to vault " + vaultName + " in " + System.getenv("AWS_REGION"));

        GlUpload glUpload = new GlUpload(archivePath, vaultName);
        System.out.println("Initialized.");

        System.out.println("Starting upload....");
        String archiveId = glUpload.upload();
        System.out.println("Upload done. Archive ID is " + archiveId);
        System.out.println("Done. Exiting...");
    }

    /**
     * Upload the specified archive to the vault
     *
     * @return The archiveId
     */
    public String upload() throws FileNotFoundException {
        return archiveTransferManager.upload(
                glacierVault,
                "Glacier backup of " + archivePath,
                new File(archivePath)
        ).getArchiveId();
    }
}
