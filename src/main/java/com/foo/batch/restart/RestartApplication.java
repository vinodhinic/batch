package com.foo.batch.restart;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableBatchProcessing
public class RestartApplication {
    // run it in persistent datasource so that the repository can look into the previous runs
    public static void main(String[] args) {
        SpringApplication.run(RestartApplication.class, args);
    }
}
