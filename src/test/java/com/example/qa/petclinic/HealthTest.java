package com.example.qa.petclinic;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("PetClinic REST API")
@Feature("Health check")
class HealthTest extends ApiTest {
    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Проверка работоспособности: сервис возвращает статус UP")
    void serviceIsHealthy() {
        Response response = request().get("/actuator/health");

        response.then().statusCode(200);
        assertThat(response.jsonPath().getString("status")).isEqualTo("UP");
    }
}
