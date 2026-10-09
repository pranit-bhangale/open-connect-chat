package com.pranit.connect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OpenConnectApplication {

    static void main(String[] args) {
        SpringApplication.run(OpenConnectApplication.class, args);
    }

}
