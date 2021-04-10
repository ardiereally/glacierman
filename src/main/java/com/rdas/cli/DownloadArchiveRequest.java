package com.rdas.cli;

public class DownloadArchiveRequest {

    private String archiveId;

    private String localFileName;

    private long fileSize;

    public DownloadArchiveRequest() {
        // nullary for serialization
    }

    public DownloadArchiveRequest(final String archiveId, final String localFileName, final long fileSize) {
        this.archiveId = archiveId;
        this.localFileName = localFileName;
        this.fileSize = fileSize;
    }

    public String getArchiveId() {
        return archiveId;
    }

    public void setArchiveId(final String archiveId) {
        this.archiveId = archiveId;
    }

    public String getLocalFileName() {
        return localFileName;
    }

    public void setLocalFileName(final String localFileName) {
        this.localFileName = localFileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(final long fileSize) {
        this.fileSize = fileSize;
    }
}
