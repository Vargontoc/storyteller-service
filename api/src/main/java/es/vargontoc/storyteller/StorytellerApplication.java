package es.vargontoc.storyteller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class StorytellerApplication {

public static void main(String[] args) {
        SpringApplication.run(StorytellerApplication.class, args);
    }

}
