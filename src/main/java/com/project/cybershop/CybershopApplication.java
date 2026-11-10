package com.project.cybershop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CybershopApplication {

    public static void main(String[] args) {
        SpringApplication.run(CybershopApplication.class, args);
    }

}
