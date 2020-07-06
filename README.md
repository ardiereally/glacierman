# glupload

Utility for uploading archives to AWS S3 Glacier

---

### Requires
- Java 11
- An AWS account
- A pre-created S3 Glacier Vault

### Usage
Make sure your AWS environment is setup properly. For more info see [AWS docs]()

A simple setup using the environment for passing credentials can be done with:
```shell script
export AWS_ACCESS_KEY_ID=<IAM user access key id>
export AWS_SECRET_ACCESS_KEY=<IAM user access key>
export AWS_REGION=<region where the vault is located>
```
Then run the jar file
```shell script
java -jar glupload.jar -Dvault=<vault name> -Darchive=<archive path>
```
Example:
```shell script
java -jar glupload.jar -Dvault=test-vault -Darchive=test.zip
```
When the upload finishes, you should see the following message:
```shell script
Starting glupload...
Will upload test.zip to vault test-vault in us-east-1
Initialized.
Starting upload....
Upload done. Archive ID is MMz1NLg5ojnIvWCGRgqDgSmBrk8871bdX1s61hLCwEN1kAb7B1L-u-qGcLxjkYFZUdAyiX-cOrWhzSNEYHfTqQ9JaaXNPX-UmkF-95w8JiNHnoYPUow_AnGyNf6hfi4973Q0tcuTkA
Done. Exiting...
```

### Build & packaging
Build using maven
```shell script
mvn clean package
```
