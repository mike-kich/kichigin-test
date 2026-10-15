# PetClinic API tests

Отдельный тестовый проект для REST API [Spring PetClinic REST](https://github.com/spring-petclinic/spring-petclinic-rest).
Тесты работают по HTTP против отдельно запущенного приложения.

## Стек

- Java 17 (проверено на JDK 17.0.12)
- Maven 3.9.9; Maven Wrapper включён
- Spring Boot 3.4.0
- JUnit 5, RestAssured, AssertJ и Hamcrest

## Запуск Spring PetClinic REST

С Docker:

```shell
docker run --rm -p 9966:9966 springcommunity/spring-petclinic-rest:3.4.0
```

Без Docker, в отдельном каталоге (Java 17 и Maven):

```shell
git clone --branch v3.4.0 --depth 1 https://github.com/spring-petclinic/spring-petclinic-rest.git
cd spring-petclinic-rest
mvn spring-boot:run
```

Дождитесь HTTP 200 и поля `status = UP`:
<http://localhost:9966/petclinic/actuator/health>.
Swagger UI: <http://localhost:9966/petclinic/swagger-ui.html>.
Приложение использует H2 в памяти; аутентификация по умолчанию отключена.
Тесты проверены против версии 3.4.0; она зафиксирована для воспроизводимости.

## Запуск тестов

Настройте `JAVA_HOME` на JDK 17+. Приложение должно быть запущено заранее.

С установленным Maven:

```shell
mvn clean test
mvn clean test -DbaseUrl=http://localhost:9966/petclinic
```

Windows, без установленного Maven:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean test '-DbaseUrl=http://localhost:9966/petclinic'
```

Linux/macOS:

```shell
./mvnw clean test
./mvnw clean test -DbaseUrl=http://localhost:9966/petclinic
```

Первый запуск скачивает Maven и зависимости из публичного Maven Central.
Закрытые сервисы и секреты не требуются.
URL по умолчанию задан в `src/test/resources/application.properties`;
`-DbaseUrl` переопределяет его. URL должен включать `/petclinic`.
Таймаут соединения — 5 секунд, ответа — 10 секунд.

## Проверки

| Тест | Ожидаемый результат |
| --- | --- |
| Health check | GET `/actuator/health`: HTTP 200, `status = UP` |
| Owner CRUD | POST 201 и проверка полей/ID/Location; GET 200; PUT 204 и повторное чтение изменённых данных; DELETE 204; GET 404 |
| Невалидный owner | POST с пустыми обязательными полями: HTTP 400 и JSON ошибки валидации с проверкой статуса, типа и описания |

Итого 3 независимых теста. CRUD — один полный сценарий без зависимости
от порядка тестовых методов. Он создаёт собственные данные;
`@AfterEach` удаляет созданного owner при падении промежуточной проверки.
Предустановленные владельцы не изменяются.
Недоступное приложение приводит к падению тестов, а не к их пропуску.
Результаты Maven Surefire: `target/surefire-reports`.

## IntelliJ IDEA

Откройте проект как Maven-проект и выберите JDK 17.
Конфигурация **All API tests** запускает `clean test`.
В Runner → JRE выберите свой JDK, если он называется иначе, чем `17`.
Также можно запускать `HealthTest`, `OwnerTest` или весь пакет из редактора.
Для другого URL добавьте `-DbaseUrl=...` в VM options JUnit-конфигурации.
Spring Boot создаёт только тестовый контекст без собственного веб-сервера.
