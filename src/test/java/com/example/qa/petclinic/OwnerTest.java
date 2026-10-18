package com.example.qa.petclinic;

import com.example.qa.petclinic.model.OwnerFields;
import com.example.qa.petclinic.model.OwnerResponse;
import com.example.qa.petclinic.model.TestData;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@Epic("PetClinic REST API")
@Feature("Owners")
class OwnerTest extends ApiTest {
    private static final Logger log = LoggerFactory.getLogger(OwnerTest.class);

    private static final String OWNERS = "/api/owners";
    private static final String OWNER = "/api/owners/{ownerId}";
    private final List<Integer> createdOwnerIds = new ArrayList<>();

    /** Best-effort очистка: не роняет тест, если владелец уже удалён или удалить не удалось. */
    @AfterEach
    void deleteCreatedOwners() {
        for (Integer id : createdOwnerIds) {
            try {
                int status = request().delete(OWNER, id).statusCode();
                if (status != 204 && status != 404) {
                    log.warn("Не удалось удалить тестового владельца id={}: HTTP {}", id, status);
                }
            } catch (RuntimeException e) {
                log.warn("Не удалось удалить тестового владельца id={}", id, e);
            }
        }
    }

    @Test
    @Story("CRUD")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Владелец: создание, чтение, обновление и удаление")
    void ownerCrudFlow() {
        OwnerFields original = TestData.owner();
        int ownerId = Allure.step("Создать владельца", () -> {
            Response createResponse = createOwner(original);
            createResponse.then().statusCode(201).contentType(ContentType.JSON);
            OwnerResponse created = createResponse.as(OwnerResponse.class);
            assertThat(created.id()).isNotNull().isPositive();
            assertThat(created.fields()).isEqualTo(original);
            assertThat(created.pets()).isEmpty();
            assertThat(createResponse.header("Location")).endsWith("/api/owners/" + created.id());
            return created.id();
        });

        Allure.step("Прочитать созданного владельца по id", () -> {
            OwnerResponse fetched = fetchOwner(ownerId);
            assertThat(fetched.id()).isEqualTo(ownerId);
            assertThat(fetched.fields()).isEqualTo(original);
            assertThat(fetched.pets()).isEmpty();
        });

        OwnerFields changed = TestData.anotherOwner();
        Allure.step("Обновить все редактируемые поля", () -> {
            jsonRequest(changed).put(OWNER, ownerId).then().statusCode(204);
        });

        Allure.step("Прочитать владельца после обновления", () -> {
            OwnerResponse afterUpdate = fetchOwner(ownerId);
            assertThat(afterUpdate.id()).isEqualTo(ownerId);
            assertThat(afterUpdate.fields()).isEqualTo(changed);
        });

        Allure.step("Удалить владельца", () -> {
            request().delete(OWNER, ownerId).then().statusCode(204);
        });

        Allure.step("Убедиться, что владелец больше не доступен", () -> {
            request().get(OWNER, ownerId).then().statusCode(404);
        });
    }

    @ParameterizedTest(name = "{0}")
    @Story("Валидация")
    @Severity(SeverityLevel.NORMAL)
    @MethodSource("invalidOwners")
    @DisplayName("Владелец: невалидные данные отклоняются с ошибкой валидации")
    void rejectsInvalidOwner(String caseName, OwnerFields invalid) {
        Response response = createOwner(invalid);

        response.then().statusCode(400).contentType("application/problem+json");
        JsonPath error = response.jsonPath();
        assertThat(error.getInt("status")).isEqualTo(400);
        assertThat(error.getString("title")).isEqualTo("MethodArgumentNotValidException");
        assertThat(error.getString("detail")).isNotBlank();
        assertThat(error.getString("type")).endsWith("/api/owners");
    }

    static Stream<Arguments> invalidOwners() {
        OwnerFields valid = TestData.owner();
        return Stream.of(
                arguments("все поля пустые", new OwnerFields("", "", "", "", "")),
                arguments("все поля null", new OwnerFields(null, null, null, null, null)),
                arguments("firstName с цифрами", valid.withFirstName("Mikhail1")),
                arguments("firstName длиннее 30 символов", valid.withFirstName("A".repeat(31))),
                arguments("lastName с апострофом", valid.withLastName("Ivan'ov")),
                arguments("address длиннее 255 символов", valid.withAddress("a".repeat(256))),
                arguments("city пустой", valid.withCity("")),
                arguments("telephone с буквами", valid.withTelephone("12345abc")),
                arguments("telephone длиннее 20 цифр", valid.withTelephone("1".repeat(21))));
    }

    /** POST владельца; если он всё-таки создан, id запоминается для очистки до любых проверок. */
    private Response createOwner(OwnerFields owner) {
        Response response = jsonRequest(owner).post(OWNERS);
        if (response.statusCode() == 201) {
            try {
                Object id = response.path("id");
                if (id instanceof Integer ownerId) {
                    createdOwnerIds.add(ownerId);
                }
            } catch (RuntimeException e) {
                log.warn("Ответ 201 без читаемого id: {}", response.asString(), e);
            }
        }
        return response;
    }

    private OwnerResponse fetchOwner(int ownerId) {
        return request().get(OWNER, ownerId)
                .then().statusCode(200).contentType(ContentType.JSON)
                .extract().as(OwnerResponse.class);
    }

}
