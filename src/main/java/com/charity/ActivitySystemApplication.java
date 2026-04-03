package com.charity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableCaching
@EnableAsync
@EnableScheduling
@EnableElasticsearchRepositories(basePackages = "com.charity.modules.activity.repository")
@SpringBootApplication
public class ActivitySystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActivitySystemApplication.class, args);
    }

}
