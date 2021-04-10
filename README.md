# Glacier-manager

Utility for uploading/downloading/listing archives to AWS S3 Glacier

---

### Requires

- Java 11
- An AWS account
- A pre-created S3 Glacier Vault

### Usage

Make sure your AWS environment is setup properly. For more info see [AWS docs]()

A simple setup using the environment for passing credentials can be done with a file. Put your credentials in a file
called `credentials.json`:

```json
{
  "accessKeyId": "<your access key id>",
  "secretAccessKey": "<your secret access key>",
  "region": "<the region where your vault is>"
}
```

Then run the jar file

#### Upload

```shell script
java -jar glupload.jar upload my-vault mydata.zip
```

#### Download

```shell script
java -jar glupload.jar download my-vault download-request.json
```

The file `download-request.json` must contain the details of a archive in the following format:

```json
{
  "archiveId": "<archive-id from glacier inventory>",
  "localFileName": "mydata.zip",
  "fileSize": "<file size from glacier inventory>"
}
```

#### Inventory

```shell
java -jar glupload.jar inventory my-vault
```

The inventory data will be downloaded \& written to a local file

### Build & packaging

Build using maven

```shell script
mvn clean package
```
