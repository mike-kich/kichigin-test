package com.example.qa.petclinic;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HealthTest extends ApiTest {
    @Test
    @DisplayName("Проверка работоспособности: сервис возвращает статус UP")
    void serviceIsHealthy() {
        Response response = request().get("/actuator/health");

        response.then().statusCode(200);
        assertThat(response.jsonPath().getString("status")).isEqualTo("UP");
    }
}
