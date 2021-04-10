package com.rdas.common;

import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressEventType;

import java.io.File;

public class Utilities {

    public static final String BACKUP_DESCRIPTION_PREFIX = "Glacier backup of ";

    /**
     * Generate the archive description for glacier from the file path.
     * DO NOT CHANGE THE LOGIC HERE
     *
     * @param archivePath the archive path
     * @return The description
     */
    public static String generateArchiveDescription(final File archivePath) {
        return BACKUP_DESCRIPTION_PREFIX + archivePath;
    }

    public static class ProgressLogger {
        private final long totalBytes;
        private long transferredBytes;
        private long lastLoggedCompletionPercent = -1;

        public ProgressLogger(final long totalBytes) {
            this.totalBytes = totalBytes;
            this.transferredBytes = 0;
        }

        public void logProgress(final ProgressEvent progressEvent) {
            if (progressEvent.getEventType() == ProgressEventType.TRANSFER_STARTED_EVENT) {
                System.out.print("Started...");
            }
            if (progressEvent.getBytesTransferred() != 0) {
                transferredBytes += progressEvent.getBytesTransferred();
                final double completion = (double) transferredBytes / (double) totalBytes;
                final long completionPercent = Math.round(completion * 100);
                if (completionPercent % 5 == 0) {
                    if (completionPercent == lastLoggedCompletionPercent) {
                        return;
                    }
                    System.out.print(String.format("%d%%...", completionPercent));
                    lastLoggedCompletionPercent = completionPercent;
                }
            }
            if (progressEvent.getEventType() == ProgressEventType.TRANSFER_COMPLETED_EVENT) {
                System.out.println("Done!");
            }
        }
    }
}
