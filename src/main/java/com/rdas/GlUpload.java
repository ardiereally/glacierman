package com.rdas;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressEventType;
import com.amazonaws.services.glacier.AmazonGlacier;
import com.amazonaws.services.glacier.AmazonGlacierClientBuilder;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManager;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManagerBuilder;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Uploader of archives to AWS Glacier
 */
public class GlUpload {
    private final String archivePath;
    private final String glacierVault;
    private final ArchiveTransferManager archiveTransferManager;

    public GlUpload(String archivePath, String glacierVault, Credentials creds) {
        this.archivePath = archivePath;
        this.glacierVault = glacierVault;

        AmazonGlacier client = AmazonGlacierClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(
                        new BasicAWSCredentials(creds.getAccessKeyId(), creds.getSecretAccessKey())))
                .withRegion(creds.getRegion())
                .build();
        AmazonSQS sqsClient = AmazonSQSClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(creds.getAccessKeyId(), creds.getSecretAccessKey()))).withRegion(creds.getRegion()).build();
        AmazonSNS snsClient = AmazonSNSClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(creds.getAccessKeyId(), creds.getSecretAccessKey()))).withRegion(creds.getRegion()).build();
        this.archiveTransferManager = new ArchiveTransferManagerBuilder()
                .withGlacierClient(client)
                .withSqsClient(sqsClient)
                .withSnsClient(snsClient)
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

        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new FileReader("credentials.json"));
        Credentials creds = gson.fromJson(reader, Credentials.class);

        System.out.println("Will upload " + archivePath + " to vault " + vaultName + " in " + creds.getRegion());

        double sizeMb = new File(archivePath).length() / (1024.0 * 1024);
        GlUpload glUpload = new GlUpload(archivePath, vaultName, creds);
        System.out.println("Initialized. Archive size is: " + sizeMb + " MB");

        long start = System.currentTimeMillis();
        String archiveId = glUpload.upload();
        System.out.println("Archive ID is " + archiveId);
        double duration = (System.currentTimeMillis() - start) / 1000.0;
        double speed = sizeMb / duration;
        System.out.println("Uploaded " + sizeMb + " MBs in " + duration + " seconds at an average speed of " + speed + " MB/s");
        System.out.println("Exiting...");
    }

    /**
     * Upload the specified archive to the vault
     *
     * @return The archiveId
     */
    public String upload() {
        File archiveFile = new File(archivePath);
        ProgressLogger progressLogger = new ProgressLogger(archiveFile.length());
        return archiveTransferManager.upload(
                "-",
                glacierVault,
                "Glacier backup of " + archivePath,
                archiveFile,
                progressLogger::logProgress
        ).getArchiveId();
    }

    public static class ProgressLogger {
        private final long totalBytes;
        private long transferredBytes;
        private long lastLoggedCompletionPercent = -1;

        public ProgressLogger(long totalBytes) {
            this.totalBytes = totalBytes;
            this.transferredBytes = 0;
        }

        public void logProgress(ProgressEvent progressEvent) {
            if (progressEvent.getEventType() == ProgressEventType.TRANSFER_STARTED_EVENT) {
                System.out.print("Started...");
            }
            if (progressEvent.getBytesTransferred() != 0) {
                transferredBytes += progressEvent.getBytesTransferred();
                double completion = (double) transferredBytes / (double) totalBytes;
                long completionPercent = Math.round(completion * 100);
                if (completionPercent % 5 == 0) {
                    if (completionPercent == lastLoggedCompletionPercent) {
                        return;
                    }
                    System.out.print(String.format("%d%%...", completionPercent));
                    lastLoggedCompletionPercent = completionPercent;
                }
            }
            if (progressEvent.getEventType() == ProgressEventType.TRANSFER_COMPLETED_EVENT) {
                System.out.println("Done!");
            }
        }
    }
}
