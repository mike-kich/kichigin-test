package com.example.qa.petclinic.config;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootConfiguration
public class ApiTestConfiguration {
    @Bean
    RequestSpecification requestSpecification(@Value("${baseUrl}") String baseUrl) {
        return new RequestSpecBuilder()
                .setBaseUri(baseUrl.replaceAll("/+$", ""))
                .setContentType(ContentType.JSON)
                .setConfig(RestAssuredConfig.config()
                        .httpClient(HttpClientConfig.httpClientConfig()
                                .setParam("http.connection.timeout", 5000)
                                .setParam("http.socket.timeout", 10000)))
                .build();
    }
}
