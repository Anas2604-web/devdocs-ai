package com.devdocsai.ingestion;

import com.devdocsai.common.DevDocsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
public class S3Service {

    private static final Logger log = LoggerFactory.getLogger(S3Service.class);

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * Uploads a spec file to S3.
     * @return the S3 key (path) where the file was stored
     */
    public String uploadSpecFile(String tenantId, MultipartFile file) {
        String key = "specs/" + tenantId + "/" + UUID.randomUUID() + "/" + file.getOriginalFilename();
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
            log.info("Uploaded spec to S3: {}", key);
            return key;

        } catch (IOException e) {
            log.error("Failed to upload file to S3", e);
            throw new DevDocsException("UPLOAD_FAILED", "Failed to upload file.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Reads a file from S3 and returns its content as a String.
     */
    public String readFileAsString(String s3Key) {
        try {
            return s3Client.getObjectAsBytes(b -> b.bucket(bucket).key(s3Key))
                    .asUtf8String();
        } catch (Exception e) {
            log.error("Failed to read file from S3: {}", s3Key, e);
            throw new DevDocsException("S3_READ_FAILED", "Failed to read spec file.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Deletes a file from S3 when a spec is deleted.
     */
    public void deleteFile(String s3Key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(s3Key).build());
            log.info("Deleted from S3: {}", s3Key);
        } catch (Exception e) {
            log.warn("Failed to delete S3 file: {} — {}", s3Key, e.getMessage());
            // Don't throw — deletion failure is non-critical
        }
    }
}
