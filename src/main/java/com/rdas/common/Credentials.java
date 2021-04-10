package com.rdas.common;

public class Credentials {
    private String accessKeyId;
    private String secretAccessKey;
    private String region;

    public Credentials() {
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(final String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    public void setSecretAccessKey(final String secretAccessKey) {
        this.secretAccessKey = secretAccessKey;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(final String region) {
        this.region = region;
    }
}
