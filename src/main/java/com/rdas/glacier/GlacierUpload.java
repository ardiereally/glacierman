package com.rdas.glacier;

import com.rdas.common.ArchiveInfo;
import com.rdas.common.Credentials;
import com.rdas.common.Utilities;

import static com.rdas.common.Utilities.generateArchiveDescription;

/**
 * Uploader of archives to AWS Glacier
 */
public class GlacierUpload extends GlacierTransfer {

    public GlacierUpload(final ArchiveInfo archiveInfo, final Credentials creds) {
        super(archiveInfo, creds);
    }

    /**
     * Upload the specified archive to the vault
     *
     * @return The archiveId
     */
    public String upload() {
        final Utilities.ProgressLogger progressLogger = new Utilities.ProgressLogger(archiveInfo.getLocalArchiveFile().length());
        return archiveTransferManager.upload(
                "-",
                archiveInfo.getVaultName(),
                generateArchiveDescription(archiveInfo.getLocalArchiveFile()),
                archiveInfo.getLocalArchiveFile(),
                progressLogger::logProgress
        ).getArchiveId();
    }

}
