package edu.jlu.intellilearnhub.server.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("intellilearn-hub.minio")
public class MinioConfigurationProperties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;
}
