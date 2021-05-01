package com.rdas.glacier;

import com.amazonaws.services.glacier.model.DeleteArchiveRequest;
import com.rdas.common.ArchiveInfo;
import com.rdas.common.Credentials;

public class GlacierDelete extends GlacierTransfer {

    public GlacierDelete(final ArchiveInfo archiveInfo, final Credentials creds) {
        super(archiveInfo, creds);
    }

    /**
     * Delete the archive from Glacier
     */
    public void delete() {
        this.glacierClient.deleteArchive(
                new DeleteArchiveRequest(
                        "-",
                        archiveInfo.getVaultName(),
                        archiveInfo.getRemoteArchiveId()
                )
        );
    }
}
