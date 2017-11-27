package javatraining;

import javatraining.configuration.HibernateConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication
@EnableWebMvc
@ComponentScan
@Import(HibernateConfiguration.class)
public class InventoryApplication
{
    public static void main( String[] args ) {
        SpringApplication.run(InventoryApplication.class, args);
    }
}
