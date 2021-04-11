package com.rdas.glacier;

import com.amazonaws.services.glacier.model.*;
import com.rdas.common.ArchiveInfo;
import com.rdas.common.Credentials;
import com.rdas.common.Utilities;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Downloader of AWS Glacier archives
 */
public class GlDownload extends GlTransfer {

    public GlDownload(final ArchiveInfo archiveInfo, final Credentials creds) {
        super(archiveInfo, creds);
    }

    private String startJob() {
        final InitiateJobRequest initJobRequest = new InitiateJobRequest()
                .withVaultName(archiveInfo.getVaultName())
                .withJobParameters(
                        new JobParameters()
                                .withType("archive-retrieval")
                                .withArchiveId(archiveInfo.getRemoteArchiveId())
                                .withTier("Standard")
                                .withDescription("Downloading " + archiveInfo.getLocalArchiveFile().getName())
                );

        final InitiateJobResult initJobResult = glacierClient.initiateJob(initJobRequest);
        return initJobResult.getJobId();
    }

    private void waitForJobCompletion(final String job) throws InterruptedException {
        System.out.println("Will wait for archive retrieval job " + job + " to complete");
        final long start = System.currentTimeMillis();
        boolean done;
        do {
            TimeUnit.MINUTES.sleep(5);
            System.out.print(".");
            final DescribeJobResult result = glacierClient.describeJob(new DescribeJobRequest().withJobId(job).withVaultName(archiveInfo.getVaultName()));
            done = result.getCompleted();
            if ("Failed".equalsIgnoreCase(result.getStatusCode())) {
                throw new RuntimeException("Job failed! Status message: " + result.getStatusMessage());
            }
        } while (!done);
        System.out.println();
        final long duration = System.currentTimeMillis() - start;
        System.out.println("Job completed in " + ((double) duration) / (1000 * 60) + " minutes");
    }


    /**
     * Prepare the file for download
     *
     * @throws InterruptedException wait interrupted
     */
    public String prepareArchive() throws InterruptedException {
        final String jobId = startJob();
        System.out.println("Started archive retrieval job. ID is " + jobId);
        waitForJobCompletion(jobId);
        System.out.println("Ready to download output");
        return jobId;
    }

    /**
     * Download the specified archive from the vault
     */
    public void download(final String jobId) {
        final Utilities.ProgressLogger progressLogger = new Utilities.ProgressLogger(archiveInfo.getFileSizeBytes());
        System.out.println("Starting download at " + new Date() + ". Archive size is: " + (archiveInfo.getFileSizeBytes() / (1024.0 * 1024)) + " MB");
        final long start = System.currentTimeMillis();
        archiveTransferManager.downloadJobOutput(
                "-",
                archiveInfo.getVaultName(),
                jobId,
                archiveInfo.getLocalArchiveFile(),
                progressLogger::logProgress
        );
        final long duration = System.currentTimeMillis() - start;
        System.out.println("Download completed in " + ((double) duration) / (1000 * 60) + " minutes");
    }
}
