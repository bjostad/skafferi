package com.skafferi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;

@SpringBootApplication
@EnableScheduling
public class SkafferiApplication {

    public static void main(String[] args) {
        // Ensure data directory exists for SQLite
        File dataDir = new File("./data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        
        SpringApplication.run(SkafferiApplication.class, args);
    }
}
