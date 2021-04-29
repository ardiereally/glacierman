package com.rdas.glacier;

import com.amazonaws.services.glacier.model.*;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.rdas.common.ArchiveInfo;
import com.rdas.common.Credentials;
import com.rdas.common.Utilities;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Downloader of AWS Glacier archives
 */
public class GlacierDownload extends GlacierTransfer {

    public GlacierDownload(final ArchiveInfo archiveInfo, final Credentials creds) {
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
        boolean done = false;
        do {
            TimeUnit.MINUTES.sleep(5);
            System.out.print(".");
            DescribeJobResult result = null;
            try {
                result = glacierClient.describeJob(new DescribeJobRequest().withJobId(job).withVaultName(archiveInfo.getVaultName()));
            } catch (final AmazonGlacierException exc) {
                if ("ServiceUnavailableException".equals(exc.getErrorCode()) || "ThrottlingException".equals(exc.getErrorCode())) {
                    continue;
                }
                throw exc;
            }
            done = result.getCompleted();
            if ("Failed".equalsIgnoreCase(result.getStatusCode())) {
                throw new RuntimeException("Job failed! Status message: " + result.getStatusMessage());
            }
        } while (!done);
        System.out.println();
        final long duration = System.currentTimeMillis() - start;
        System.out.println("Job completed in " + ((double) duration) / (1000 * 60) + " minutes");
    }

    private String jobFileName() {
        return "job_" + archiveInfo.getLocalArchiveFile() + ".json";
    }

    private void writeJobFile(final String jobId) throws IOException {
        final JobInfo jobInfo = new JobInfo(archiveInfo.getRemoteArchiveId(), jobId);
        final Gson gson = new Gson();
        final Writer writer = Files.newBufferedWriter(Paths.get(jobFileName()));
        gson.toJson(jobInfo, writer);
        writer.flush();
        writer.close();
    }

    private String readIdFromJobFile() throws FileNotFoundException {
        final Gson gson = new Gson();
        final JsonReader reader = new JsonReader(new FileReader(jobFileName()));
        final JobInfo jobInfo = gson.fromJson(reader, JobInfo.class);
        if (!jobInfo.getArchiveId().equals(archiveInfo.getRemoteArchiveId())) {
            System.out.println("ArchiveId mismatch for same archive name!");
            return null;
        }
        return jobInfo.getJobId();
    }

    private boolean jobFileExists() throws IOException {
        final File jobFile = new File(jobFileName());
        if (jobFile.exists()) {
            final BasicFileAttributes attributes = Files.readAttributes(jobFile.toPath(), BasicFileAttributes.class);
            final Instant lastModifiedTime = attributes.lastModifiedTime().toInstant();
            // job file can't be more than 18 hours old
            return Instant.now().minus(18L, ChronoUnit.HOURS).isBefore(lastModifiedTime);
        }
        return false;
    }

    private void deleteJobFile() {
        if (new File(jobFileName()).delete()) {
            System.out.println("Job file deleted");
        }
    }

    private String startNewJob() throws IOException {
        final String jobId = startJob();
        writeJobFile(jobId);
        return jobId;
    }

    /**
     * Prepare the file for download
     *
     * @throws InterruptedException wait interrupted
     */
    public String prepareArchive() throws InterruptedException, IOException {
        String jobId = null;
        if (jobFileExists()) {
            jobId = readIdFromJobFile();
            if (jobId == null) {
                jobId = startNewJob();
            }
        } else {
            jobId = startNewJob();
        }
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
        deleteJobFile();
    }

    public static class JobInfo {
        private String archiveId;
        private String jobId;

        public JobInfo() {
            // required for deserialization
        }

        public JobInfo(final String archiveId, final String jobId) {
            this.archiveId = archiveId;
            this.jobId = jobId;
        }

        public String getArchiveId() {
            return archiveId;
        }

        public void setArchiveId(final String archiveId) {
            this.archiveId = archiveId;
        }

        public String getJobId() {
            return jobId;
        }

        public void setJobId(final String jobId) {
            this.jobId = jobId;
        }
    }

}
