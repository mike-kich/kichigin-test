package com.example.qa.petclinic;

import com.example.qa.petclinic.model.OwnerFields;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;

class OwnerTest extends ApiTest {
    private Integer ownerId;

    @AfterEach
    void removeCreatedOwner() {
        if (ownerId != null) {
            request().delete("/api/owners/{id}", ownerId).then().statusCode(204);
        }
    }

    @Test
    @DisplayName("Владелец: создание, чтение, обновление и удаление")
    void ownerCrudFlow() {
        OwnerFields original = new OwnerFields("Mikhail", "Ivanov",
                "ul. Lenina, 12", "Samara", "9001234567");
        Response created = request().body(original).post("/api/owners");
        // Сохраняем ID до проверок, чтобы удалить владельца и при падении теста.
        if (created.statusCode() == 201) {
            ownerId = created.jsonPath().getInt("id");
        }
        created.then().statusCode(201).body("id", greaterThan(0));
        assertThat(ownerId).isNotNull();
        assertOwner(created, original);
        assertThat(created.header("Location")).endsWith("/api/owners/" + ownerId);

        Response fetched = request().get("/api/owners/{id}", ownerId);
        fetched.then().statusCode(200).body("id", equalTo(ownerId));
        assertOwner(fetched, original);

        OwnerFields updated = new OwnerFields("Aleksandr", original.lastName(),
                "ul. Pushkina, 34", "Kazan", "9107654321");
        request().body(updated).put("/api/owners/{id}", ownerId)
                .then().statusCode(204);
        Response afterUpdate = request().get("/api/owners/{id}", ownerId);
        afterUpdate.then().statusCode(200).body("id", equalTo(ownerId));
        assertOwner(afterUpdate, updated);

        request().delete("/api/owners/{id}", ownerId).then().statusCode(204);
        int deletedId = ownerId;
        ownerId = null;
        request().get("/api/owners/{id}", deletedId).then().statusCode(404);
    }

    @Test
    @DisplayName("Владелец: пустые обязательные поля возвращают ошибку валидации")
    void rejectsInvalidOwner() {
        OwnerFields invalid = new OwnerFields("", "", "", "", "");
        Response response = request().body(invalid).post("/api/owners");
        if (response.statusCode() == 201) {
            ownerId = response.jsonPath().getInt("id");
        }
        response.then().statusCode(400)
                .contentType("application/problem+json")
                .body("status", equalTo(400))
                .body("title", equalTo("MethodArgumentNotValidException"))
                .body("detail", not(isEmptyOrNullString()))
                .body("type", org.hamcrest.Matchers.endsWith("/api/owners"));
    }

    private static void assertOwner(Response response, OwnerFields expected) {
        assertThat(response.jsonPath().getString("firstName")).isEqualTo(expected.firstName());
        assertThat(response.jsonPath().getString("lastName")).isEqualTo(expected.lastName());
        assertThat(response.jsonPath().getString("address")).isEqualTo(expected.address());
        assertThat(response.jsonPath().getString("city")).isEqualTo(expected.city());
        assertThat(response.jsonPath().getString("telephone")).isEqualTo(expected.telephone());
    }
}
