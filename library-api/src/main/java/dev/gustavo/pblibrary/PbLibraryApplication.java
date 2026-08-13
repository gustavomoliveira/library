package dev.gustavo.pblibrary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class PbLibraryApplication {

    public static void main(String[] args) {
        SpringApplication.run(PbLibraryApplication.class, args);
    }

}
