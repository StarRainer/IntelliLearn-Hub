package edu.jlu.intellilearnhub.server.config;


import edu.jlu.intellilearnhub.server.config.properties.MinioConfigurationProperties;
import io.minio.MinioClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MinioConfigurationProperties.class)
public class MinioConfiguration {
    @Bean
    public MinioClient minioClient(MinioConfigurationProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(),
                        properties.getSecretKey()
                ).build();
    }
}
