package ru.anikson.cloudfilestorage.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;



@Configuration
public class RedisTestConfig {
    private static final Logger logger = LoggerFactory.getLogger(RedisTestConfig.class);

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private int redisPort;

    @Value("${spring.redis.password}")
    private String redisPassword;

    @PostConstruct
    public void logRedisConfig() {
        logger.info("Redis Host: {}", redisHost);
        logger.info("Redis Port: {}", redisPort);
        logger.info("Redis Password: {}", redisPassword);
    }
}