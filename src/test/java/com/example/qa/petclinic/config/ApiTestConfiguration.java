package com.example.qa.petclinic.config;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

@SpringBootConfiguration
public class ApiTestConfiguration {
    private static final Logger log = LoggerFactory.getLogger(ApiTestConfiguration.class);
    private static final int PETCLINIC_PORT = 9966;
    private static final String CONTEXT_PATH = "/petclinic";

    /**
     * PetClinic в Docker. Создаётся только при petclinic.testcontainers.enabled=true.
     * Spring кеширует тестовый контекст, поэтому контейнер стартует один раз на весь прогон
     * и останавливается при закрытии контекста.
     */
    @Bean(destroyMethod = "stop")
    @ConditionalOnProperty(name = "petclinic.testcontainers.enabled", havingValue = "true")
    GenericContainer<?> petclinicContainer(@Value("${petclinic.testcontainers.image}") String image) {
        GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse(image))
                .withExposedPorts(PETCLINIC_PORT)
                .waitingFor(Wait.forHttp(CONTEXT_PATH + "/actuator/health")
                        .forPort(PETCLINIC_PORT)
                        .forStatusCode(200)
                        .withStartupTimeout(Duration.ofMinutes(3)));
        log.info("Запуск PetClinic в Docker: {}", image);
        container.start();
        return container;
    }

    @Bean
    RequestSpecification requestSpecification(@Value("${baseUrl}") String configuredBaseUrl,
                                              @Value("${api.logging.all:false}") boolean logAll,
                                              ObjectProvider<GenericContainer<?>> petclinicContainer) {
        GenericContainer<?> container = petclinicContainer.getIfAvailable();
        String baseUrl = container == null
                ? configuredBaseUrl
                : "http://" + container.getHost() + ":" + container.getMappedPort(PETCLINIC_PORT) + CONTEXT_PATH;
        log.info("PetClinic base URL: {}", baseUrl);

        LogConfig logConfig = LogConfig.logConfig().enablePrettyPrinting(true);
        if (!logAll) {
            // Запрос и ответ печатаются, только если упала проверка RestAssured (then()...).
            logConfig = logConfig.enableLoggingOfRequestAndResponseIfValidationFails(LogDetail.ALL);
        }

        // Content-Type не задаётся глобально: он нужен только запросам с телом (см. ApiTest#jsonRequest).
        RequestSpecBuilder builder = new RequestSpecBuilder()
                .setBaseUri(baseUrl.replaceAll("/+$", ""))
                .setConfig(RestAssuredConfig.config()
                        .httpClient(HttpClientConfig.httpClientConfig()
                                .setParam("http.connection.timeout", 5000)
                                .setParam("http.socket.timeout", 10000))
                        .logConfig(logConfig))
                // Каждый запрос и ответ прикладывается к шагу в Allure-отчёте.
                .addFilter(new AllureRestAssured());
        if (logAll) {
            builder.addFilter(new RequestLoggingFilter(LogDetail.ALL))
                    .addFilter(new ResponseLoggingFilter(LogDetail.ALL));
        }
        return builder.build();
    }
}
