package dev.parallaxsports;

import org.springframework.boot.SpringApplication;

public class TestParallaxSportsApiApplication {

    public static void main(String[] args) {
        SpringApplication.from(ParallaxSportsApiApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
