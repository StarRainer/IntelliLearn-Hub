package edu.jlu.intellilearnhub.server.service.impl;

import edu.jlu.intellilearnhub.server.config.properties.MinioConfigurationProperties;
import edu.jlu.intellilearnhub.server.service.FileUploadService;
import io.minio.*;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;


@Service
@Slf4j
public class FileUploadServiceImpl implements FileUploadService {
    @Autowired
    private MinioClient minioClient;
    @Autowired
    private MinioConfigurationProperties minioConfigurationProperties;

    @Override
    public String upload(String folder, MultipartFile file) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        String bucketName = minioConfigurationProperties.getBucketName();

        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucketName)
                .build()
        );

        if (!bucketExists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            String config = """
                        {
                              "Statement" : [ {
                                "Action" : "s3:GetObject",
                                "Effect" : "Allow",
                                "Principal" : "*",
                                "Resource" : "arn:aws:s3:::%s/*"
                              } ],
                              "Version" : "2012-10-17"
                        }
                    """.formatted(bucketName);
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(bucketName)
                    .config(config)
                    .build());
        }

        String objectName = folder + "/"
                + new SimpleDateFormat("yyyy_MM_dd").format(new Date()) + "/"
                + UUID.randomUUID().toString().replace("-", "") + "_"
                + file.getOriginalFilename();
        log.debug("文件上传使用的文件对象名={}", objectName);

        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketName)
                .contentType(file.getContentType())
                .object(objectName)
                .stream(file.getInputStream(), file.getSize(), -1)
                .build()
        );

        String endpoint = minioConfigurationProperties.getEndpoint();

        String url = String.join("/", endpoint, bucketName, objectName);
        log.info("文件上传成功，回显地址={}", url);
        return url;
    }
}
