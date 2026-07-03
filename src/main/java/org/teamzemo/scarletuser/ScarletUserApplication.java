package org.teamzemo.scarletuser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ScarletUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScarletUserApplication.class, args);
    }

}
