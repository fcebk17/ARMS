package ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ComponentScan("ntou.cse.soselab.automigrationfrommonolithictomicroservicesmono")
@PropertySource("classpath:application.properties")
public class GptConfig {
}
