package com.rdas.glacier;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.glacier.AmazonGlacier;
import com.amazonaws.services.glacier.AmazonGlacierClientBuilder;
import com.amazonaws.services.glacier.model.*;
import com.rdas.common.Credentials;

import java.io.*;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class GlacierInventory {
    private final AmazonGlacier client;

    public GlacierInventory(final Credentials credentials) {
        client = AmazonGlacierClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(
                        new BasicAWSCredentials(credentials.getAccessKeyId(), credentials.getSecretAccessKey())))
                .withRegion(credentials.getRegion())
                .build();
    }

    private InventoryJob start(final String vault) {
        final InitiateJobRequest initJobRequest = new InitiateJobRequest()
                .withVaultName(vault)
                .withJobParameters(
                        new JobParameters()
                                .withType("inventory-retrieval")
                );

        final InitiateJobResult initJobResult = client.initiateJob(initJobRequest);
        return new InventoryJob(vault, initJobResult.getJobId());
    }

    private void pollStatus(final InventoryJob job) throws InterruptedException {
        boolean done;
        do {
            TimeUnit.MINUTES.sleep(2);
            System.out.print(".");
            final DescribeJobResult result = client.describeJob(new DescribeJobRequest().withJobId(job.getJobId()).withVaultName(job.getVault()));
            done = result.getCompleted();
            if ("Failed".equals(result.getStatusCode())) {
                throw new RuntimeException("Job failed! Status message: " + result.getStatusMessage());
            }
        } while (!done);
        System.out.println();
    }

    private void getOutput(final InventoryJob job) throws IOException {
        final String fileName = job.getVault() + "_inventory_" + new Date().toString().replace(' ', '_').replace(':', '.') + ".json";
        final GetJobOutputRequest jobOutputRequest = new GetJobOutputRequest()
                .withVaultName(job.getVault())
                .withJobId(job.getJobId());
        final GetJobOutputResult jobOutputResult = client.getJobOutput(jobOutputRequest);
        final FileWriter fileWriter = new FileWriter(fileName);
        String inputLine;
        try (final BufferedWriter out = new BufferedWriter(fileWriter); final BufferedReader in = new BufferedReader(new InputStreamReader(jobOutputResult.getBody()))) {
            while ((inputLine = in.readLine()) != null) {
                out.write(inputLine);
            }
        } catch (final IOException e) {
            throw new AmazonClientException("Unable to save archive", e);
        }
        System.out.println("Retrieved inventory to " + fileName);
    }

    public void inventory(final String vaultName) throws InterruptedException, IOException {
        final GlacierInventory.InventoryJob job = start(vaultName);
        System.out.println("Job started: " + job.getJobId() + " (at " + new Date() + ")");
        final long start = System.currentTimeMillis();
        pollStatus(job);
        final long duration = System.currentTimeMillis() - start;
        System.out.printf("Job completed (at " + new Date() + " ) in %f minutes", ((double) duration) / (1000 * 60));
        getOutput(job);
    }

    public static class InventoryJob {
        private final String vault;
        private final String jobId;

        public InventoryJob(final String vault, final String jobId) {
            this.vault = vault;
            this.jobId = jobId;
        }

        public String getVault() {
            return vault;
        }

        public String getJobId() {
            return jobId;
        }
    }

}
