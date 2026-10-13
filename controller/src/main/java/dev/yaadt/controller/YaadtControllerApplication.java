package dev.yaadt.controller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class YaadtControllerApplication {

    public static void main(String[] args) {
        SpringApplication.run(YaadtControllerApplication.class, args);
    }
}
