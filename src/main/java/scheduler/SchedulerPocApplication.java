package scheduler;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// Run this with -Dspring.profiles.active=scheduler
@SpringBootApplication
@EnableBatchProcessing
@EnableScheduling
public class SchedulerPocApplication {
    public static void main(String[] args) {
        SpringApplication
                .run(SchedulerPocApplication.class, args);
    }
}
