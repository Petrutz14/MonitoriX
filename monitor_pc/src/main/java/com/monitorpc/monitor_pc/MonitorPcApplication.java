package com.monitorpc.monitor_pc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MonitorPcApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonitorPcApplication.class, args);
    }

}
