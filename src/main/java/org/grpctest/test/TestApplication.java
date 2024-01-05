package org.grpctest.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class TestApplication {

    public static void main(String[] args) throws IOException, InterruptedException {
        SpringApplication.run(TestApplication.class, args);
    }

}
