package com.rdas.common;

import java.io.File;

public class ArchiveInfo {

    private final String vaultName;
    private final String remoteArchiveId;
    private final File localArchiveFile;
    private final long fileSizeBytes;

    public ArchiveInfo(final String vaultName, final String remoteArchiveId, final File localArchiveFileName, final long fileSizeBytes) {
        this.vaultName = vaultName;
        this.remoteArchiveId = remoteArchiveId;
        this.localArchiveFile = localArchiveFileName;
        this.fileSizeBytes = fileSizeBytes;
    }

    /**
     * Generate archive info for uploading a local archive
     *
     * @param vaultName   The destination vault
     * @param archivePath The path to the local archive
     * @return the info object
     */
    public static ArchiveInfo ofLocal(final String vaultName, final File archivePath) {
        return new ArchiveInfo(vaultName, null, archivePath, archivePath.length());
    }

    /**
     * Generate archive info for downloading a remote archive. The destination path for eventual download is also populated.
     *
     * @param vaultName The source vault
     * @param archiveId The ID of the archive
     * @return The info object
     */
    public static ArchiveInfo ofRemote(final String vaultName, final String archiveId, final File archivePath, final String fileSizeBytes) {
        return new ArchiveInfo(vaultName, archiveId, archivePath, Long.parseLong(fileSizeBytes));
    }

    public String getVaultName() {
        return vaultName;
    }

    public String getRemoteArchiveId() {
        return remoteArchiveId;
    }

    public File getLocalArchiveFile() {
        return localArchiveFile;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }
}
