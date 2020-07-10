package com.rdas;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.glacier.AmazonGlacier;
import com.amazonaws.services.glacier.AmazonGlacierClientBuilder;
import com.amazonaws.services.glacier.model.*;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.*;
import java.util.concurrent.TimeUnit;

public class GlacierInventory {
    private final AmazonGlacier client;
    private final String vault;

    GlacierInventory(String vault) throws FileNotFoundException {
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new FileReader("credentials.json"));
        GlUpload.Credentials creds = gson.fromJson(reader, GlUpload.Credentials.class);

        client = AmazonGlacierClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(
                        new BasicAWSCredentials(creds.getAccessKeyId(), creds.getSecretAccessKey())))
                .withRegion(creds.getRegion())
                .build();
        this.vault = vault;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        GlacierInventory inventory = new GlacierInventory("Movies");
        String jobId = inventory.start();
        System.out.println("Job started: " + jobId);
        inventory.pollStatus(jobId);
        inventory.getOutput(jobId, "Movies_Vault.json");
    }

    public String start() {
        InitiateJobRequest initJobRequest = new InitiateJobRequest()
                .withVaultName(vault)
                .withJobParameters(
                        new JobParameters()
                                .withType("inventory-retrieval")
                );

        InitiateJobResult initJobResult = client.initiateJob(initJobRequest);
        String jobId = initJobResult.getJobId();
        System.out.println(jobId);
        return jobId;
    }

    public void pollStatus(String jobId) throws InterruptedException {
        boolean done;
        do {
            TimeUnit.MINUTES.sleep(2);
            System.out.println("I'm gonna poll again");
            DescribeJobResult result = client.describeJob(new DescribeJobRequest().withJobId(jobId).withVaultName(vault));
            done = result.getCompleted();
        } while (!done);
    }

    public void getOutput(String jobId, String fileName) throws IOException {
        GetJobOutputRequest jobOutputRequest = new GetJobOutputRequest()
                .withVaultName(vault)
                .withJobId(jobId);
        GetJobOutputResult jobOutputResult = client.getJobOutput(jobOutputRequest);
        FileWriter fstream = new FileWriter(fileName);
        String inputLine;
        try (BufferedWriter out = new BufferedWriter(fstream); BufferedReader in = new BufferedReader(new InputStreamReader(jobOutputResult.getBody()))) {
            while ((inputLine = in.readLine()) != null) {
                out.write(inputLine);
            }
        } catch (IOException e) {
            throw new AmazonClientException("Unable to save archive", e);
        }
        System.out.println("Retrieved inventory to " + fileName);
    }

}
