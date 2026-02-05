package edu.jlu.intellilearnhub.server.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "intellilearn-hub.thread-pool")
public class ThreadPoolConfigurationProperties {
    private int corePoolSize;
    private int maximumPoolSize;
    private long keepAliveTime;
    private int queueCapacity;
}

