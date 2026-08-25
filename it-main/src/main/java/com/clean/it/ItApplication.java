package com.clean.it;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ItApplication {
    public static void main(String[] args){SpringApplication.run(ItApplication.class,args);}
}
