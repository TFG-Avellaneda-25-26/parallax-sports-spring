package dev.parallaxsports;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ParallaxSportsApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ParallaxSportsApiApplication.class, args);
    }

}
