package com.rdas.cli;

public class DownloadArchiveRequest {

    private String archiveId;

    private String localFileName;

    private String fileSize;

    public DownloadArchiveRequest() {
        // nullary for serialization
    }

    public DownloadArchiveRequest(final String archiveId, final String localFileName, final String fileSize) {
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

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(final String fileSize) {
        this.fileSize = fileSize;
    }
}
