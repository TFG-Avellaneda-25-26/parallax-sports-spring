package dev.parallaxsports;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories(basePackages = {"dev.parallaxsports"})
public class ParallaxSportsApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ParallaxSportsApiApplication.class, args);
    }

}
