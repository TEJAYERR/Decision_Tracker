package com.ai.decision_tracker01.config;

import com.falkordb.Driver;
import com.falkordb.FalkorDB;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FalkorConfig {

    @Bean
    public Driver falkorDriver() {
        return FalkorDB.driver(); // connects to localhost:6379 by default
    }
}