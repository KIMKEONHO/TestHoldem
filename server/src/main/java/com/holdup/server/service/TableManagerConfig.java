package com.holdup.server.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TableManagerConfig {

    @Bean
    public TableManager tableManager() {
        return new TableManager();
    }
}
