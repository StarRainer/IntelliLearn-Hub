package edu.jlu.intellilearnhub.server.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("edu.jlu.intellilearnhub.server")
public class MybatisPlusConfiguration {
}
