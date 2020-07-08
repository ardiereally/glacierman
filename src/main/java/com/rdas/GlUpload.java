package com.rdas;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
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

        GlUpload glUpload = new GlUpload(archivePath, vaultName, creds);
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

    public static class Credentials {
        private String accessKeyId;
        private String secretAccessKey;
        private String region;

        public Credentials() {
        }

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getSecretAccessKey() {
            return secretAccessKey;
        }

        public void setSecretAccessKey(String secretAccessKey) {
            this.secretAccessKey = secretAccessKey;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }
    }
}
