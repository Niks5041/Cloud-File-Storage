//package ru.anikson.cloudfilestorage.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
//import org.springframework.session.web.http.CookieSerializer;
//import org.springframework.session.web.http.DefaultCookieSerializer;
//
//@Configuration
//@EnableRedisHttpSession //активирует использование Redis для хранения сессий в приложении.
//public class RedisSessionConfig {
//
//    @Bean
//    public CookieSerializer cookieSerializer() {
//        DefaultCookieSerializer cookieSerializer = new DefaultCookieSerializer();
//        cookieSerializer.setCookieName("SESSION");
//        cookieSerializer.setCookiePath("/"); //cookie будет доступно для всего сайта
//        return cookieSerializer;
//    }
//}
