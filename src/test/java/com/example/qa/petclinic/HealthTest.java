package com.example.qa.petclinic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;

class HealthTest extends ApiTest {
    @Test
    @DisplayName("Проверка работоспособности: сервис возвращает статус UP")
    void serviceIsHealthy() {
        request().get("/actuator/health")
                .then().statusCode(200)
                .body("status", equalTo("UP"));
    }
}
