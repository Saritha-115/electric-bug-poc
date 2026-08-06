package com.aximly.retailsync_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import javax.sql.DataSource;

@Configuration
public class CloudDataSourceConfig {

    @Value("${cloud.datasource.url}")
    private String url;

    @Value("${cloud.datasource.username}")
    private String username;

    @Value("${cloud.datasource.password}")
    private String password;

    @Value("${cloud.datasource.driver-class-name}")
    private String driverClassName;

    @Bean(name = "cloudDataSource")
    public DataSource cloudDataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName(driverClassName);
        ds.setUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        return ds;
    }
}