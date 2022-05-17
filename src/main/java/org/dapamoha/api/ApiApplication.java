package org.dapamoha.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages = {
        "org.dapamoha.api",
        "org.dapamoha.shared.kafka",
        "org.dapamoha.shared.domain",
        "org.dapamoha.shared.posgresql",
})
@SpringBootApplication
public class ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }

}