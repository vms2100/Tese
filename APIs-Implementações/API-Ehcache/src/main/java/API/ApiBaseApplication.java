package API;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"API", "Repository"})
public class ApiBaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiBaseApplication.class, args);
    }
}
