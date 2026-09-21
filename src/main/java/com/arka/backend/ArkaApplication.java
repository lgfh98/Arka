package com.arka.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ArkaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArkaApplication.class, args);
    }
}
