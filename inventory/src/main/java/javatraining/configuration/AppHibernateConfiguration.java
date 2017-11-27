package javatraining.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "db", locations = "classpath:application.yaml")
@Getter
@Setter
public class AppHibernateConfiguration {

    private String dialect;
    private String showSql;
    private String formatSql;
    private String driverClassName;
    private String url;
    private String username;
    private String password;
}
