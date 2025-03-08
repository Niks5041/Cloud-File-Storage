package ru.anikson.cloudfilestorage.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class RedisConfigLogger implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(RedisConfigLogger.class);

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private String redisPort;

    @Override
    public void run(String... args) throws Exception {
        log.info("Redis configuration loaded - Host: {}, Port: {}", redisHost, redisPort);
    }
}