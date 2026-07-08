package io.github.vlovric.dazaikindle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ServerApplication{
    public static void main(String[] args) {
        // SpringApplication defaults headless to true and locks in
        // java.awt.headless before application.properties is even read, so
        // spring.main.headless=false there has no effect - it has to be set
        // directly, this early, for java.awt.Desktop (used to open run
        // artifacts in the OS filesystem) to work.
        System.setProperty("java.awt.headless", "false");
        SpringApplication.run(ServerApplication.class, args);
    }
}