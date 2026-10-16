# PetClinic API tests

Отдельный тестовый проект для REST API [Spring PetClinic REST](https://github.com/spring-petclinic/spring-petclinic-rest).
Тесты работают по HTTP против отдельно запущенного приложения.

## Стек

- Java 17 (проверено на JDK 17.0.12)
- Maven 3.9.9; Maven Wrapper включён
- Spring Boot 3.4.0
- JUnit 5 (в т.ч. параметризованные тесты), RestAssured, AssertJ

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
| Owner CRUD | POST 201: id, все поля, пустой `pets`, `Location`; GET 200 с теми же данными; PUT 204 с изменением **всех** полей и повторное чтение; DELETE 204; GET 404 |
| Невалидный owner (9 случаев) | POST → HTTP 400, `application/problem+json`, `status = 400`, тип ошибки валидации, непустой `detail` и адрес запроса |

Негативные случаи (параметризованный тест, ограничения взяты из OpenAPI-спецификации PetClinic):

| Случай | Поля с невалидными данными |
| --- | --- |
| Все поля пустые | все пять |
| Все поля `null` | все пять |
| `firstName` с цифрами | `firstName` |
| `firstName` длиннее 30 символов | `firstName` |
| `lastName` с апострофом (`Ivan'ov`) | `lastName` |
| `address` длиннее 255 символов | `address` |
| `city` пустой | `city` |
| `telephone` с буквами | `telephone` |
| `telephone` длиннее 20 цифр | `telephone` |

Итого 11 тестов: health, CRUD и 9 негативных случаев.
Тесты не зависят от порядка запуска. `TestData` содержит владельцев `Mikhail Ivanov`
и `Aleksandr Petrov` с разными значениями всех пяти полей. Телефоны состоят из 10 цифр:
это ограничение модели приложения 3.4.0, хотя OpenAPI допускает до 20.
Тесты работают только со своими созданными ID, предустановленные владельцы не изменяются.
Текст `detail` не разбирается регулярными выражениями и не сравнивается с шаблоном Spring.
`@AfterEach` удаляет всех созданных в тесте владельцев — в том числе если промежуточная
проверка упала или невалидный owner неожиданно был создан. Очистка best-effort:
ошибка удаления логируется и не роняет тест.
Недоступное приложение приводит к падению тестов, а не к их пропуску.
Результаты Maven Surefire: `target/surefire-reports`.

## IntelliJ IDEA

Откройте проект как Maven-проект и выберите JDK 17.
Конфигурация **All API tests** запускает `clean test`.
В Runner → JRE выберите свой JDK, если он называется иначе, чем `17`.
Также можно запускать `HealthTest`, `OwnerTest` или весь пакет из редактора.
Для другого URL добавьте `-DbaseUrl=...` в VM options JUnit-конфигурации.
Spring Boot создаёт только тестовый контекст без собственного веб-сервера.
