package edu.jlu.intellilearnhub.server.config;

import edu.jlu.intellilearnhub.server.config.properties.ThreadPoolConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
@EnableConfigurationProperties(ThreadPoolConfigurationProperties.class)
public class ThreadPoolConfiguration {
    @Bean
    public ThreadPoolExecutor threadPoolExecutor(ThreadPoolConfigurationProperties properties) {
        return new ThreadPoolExecutor(
                properties.getCorePoolSize(),
                properties.getMaximumPoolSize(),
                properties.getKeepAliveTime(),
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(properties.getQueueCapacity()),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }
}
