package com.rdas.glacier;

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
import com.rdas.common.ArchiveInfo;
import com.rdas.common.Credentials;

import static java.util.Objects.requireNonNull;

public abstract class GlTransfer {
    protected final ArchiveInfo archiveInfo;
    protected final AmazonGlacier glacierClient;
    protected final ArchiveTransferManager archiveTransferManager;

    public GlTransfer(final ArchiveInfo archiveInfo, final Credentials creds) {
        this.archiveInfo = requireNonNull(archiveInfo, "archive info is null");

        this.glacierClient = AmazonGlacierClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(
                        new BasicAWSCredentials(creds.getAccessKeyId(), creds.getSecretAccessKey())))
                .withRegion(creds.getRegion())
                .build();
        final AmazonSQS sqsClient = AmazonSQSClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(creds.getAccessKeyId(), creds.getSecretAccessKey()))).withRegion(creds.getRegion()).build();
        final AmazonSNS snsClient = AmazonSNSClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(creds.getAccessKeyId(), creds.getSecretAccessKey()))).withRegion(creds.getRegion()).build();
        this.archiveTransferManager = new ArchiveTransferManagerBuilder()
                .withGlacierClient(this.glacierClient)
                .withSqsClient(sqsClient)
                .withSnsClient(snsClient)
                .build();
    }
}
