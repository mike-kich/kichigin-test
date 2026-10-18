# PetClinic API tests

Отдельный тестовый проект для REST API [Spring PetClinic REST](https://github.com/spring-petclinic/spring-petclinic-rest).
Тесты работают по HTTP против отдельно запущенного приложения.

## Стек

- Java 17 (проверено на JDK 17.0.12)
- Maven 3.9.9; Maven Wrapper включён
- Spring Boot 3.4.0
- JUnit 5 (в т.ч. параметризованные тесты), RestAssured, AssertJ
- Allure 2.29.0 (allure-junit5, allure-rest-assured), allure-maven 2.12.0
- Testcontainers 1.21.4 (опционально, нужен Docker)

## Запуск Spring PetClinic REST

Есть три способа: вручную через Docker, через docker compose или автоматически
из тестов через Testcontainers (см. раздел [Testcontainers](#testcontainers)).

С Docker:

```shell
docker run --rm -p 9966:9966 springcommunity/spring-petclinic-rest:3.4.0
```

Через docker compose (файл `docker-compose.yml` в корне проекта):

```shell
docker compose up -d
# ... прогон тестов ...
docker compose down
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
Приложение использует HSQLDB в памяти; аутентификация по умолчанию отключена.
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

### Testcontainers

Тесты могут сами поднять PetClinic в Docker — запускать приложение заранее не нужно:

```shell
mvn clean test -Dpetclinic.testcontainers.enabled=true
```

Нужен запущенный Docker (Docker Desktop или Docker Engine). Контейнер
`springcommunity/spring-petclinic-rest:3.4.0` стартует один раз на весь прогон на случайном
порту; тесты ждут HTTP 200 от `/petclinic/actuator/health` (до 3 минут, первый запуск
дольше из-за скачивания образа). `-DbaseUrl` в этом режиме не используется.
Другой образ: `-Dpetclinic.testcontainers.image=...`.

### Логирование запросов и ответов

По умолчанию запрос и ответ печатаются в консоль, только если упала проверка RestAssured
(статус, Content-Type). Логировать каждый запрос и ответ:

```shell
mvn clean test -Dapi.logging.all=true
```

Независимо от этой настройки все запросы и ответы прикладываются к Allure-отчёту.

### Allure-отчёт

Результаты пишутся в `target/allure-results` при каждом `mvn test`. Отчёт:

```shell
# открыть отчёт в браузере (поднимает локальный сервер)
mvn allure:serve

# или собрать статический отчёт в target/site/allure-maven-plugin/index.html
mvn allure:report
```

Allure не нужно устанавливать: плагин сам скачивает его из Maven Central в каталог `.allure`
(он в `.gitignore`). Если тесты упали, Maven остановится до построения отчёта,
поэтому одной командой удобно так:

```shell
mvn clean test allure:report -Dmaven.test.failure.ignore=true
```

В отчёте тесты сгруппированы по Epic/Feature/Story, у CRUD-сценария есть шаги
(создание, чтение, обновление, удаление), к каждому шагу приложены HTTP-запрос и ответ.

## Проверки

| Тест | Ожидаемый результат |
| --- | --- |
| Health check | GET `/actuator/health`: HTTP 200, `status = UP` |
| Owner CRUD | POST 201: id, все поля, пустой `pets`, `Location`; GET 200 с теми же данными; PUT 204 с изменением **всех** полей и повторное чтение; DELETE 204; GET 404 |
| Невалидный owner (9 случаев) | POST → HTTP 400, `application/problem+json`, `status = 400`, `title = MethodArgumentNotValidException`, непустой `detail`, `type` заканчивается на `/api/owners` |

Негативные случаи (параметризованный тест, ограничения взяты из OpenAPI-спецификации PetClinic):

| Случай | Ожидаемые поля с ошибкой |
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

Тесты не зависят от порядка запуска. Используются данные Mikhail Ivanov и Aleksandr Petrov с 10-значными телефонами.
Каждый тест работает с созданным ID; предустановленные владельцы не изменяются.
`@AfterEach` удаляет всех созданных в тесте владельцев — в том числе если промежуточная
проверка упала или невалидный owner неожиданно был создан. Очистка best-effort:
ошибка удаления логируется и не роняет тест.
Недоступное приложение приводит к падению тестов, а не к их пропуску.
Результаты Maven Surefire: `target/surefire-reports`, Allure: `target/allure-results`.

## IntelliJ IDEA

Откройте проект как Maven-проект и выберите JDK 17.
Конфигурация **All API tests** запускает `clean test`.
В Runner → JRE выберите свой JDK, если он называется иначе, чем `17`.
Также можно запускать `HealthTest`, `OwnerTest` или весь пакет из редактора.
Для другого URL добавьте `-DbaseUrl=...` в VM options JUnit-конфигурации,
для Testcontainers — `-Dpetclinic.testcontainers.enabled=true`.
Spring Boot создаёт только тестовый контекст без собственного веб-сервера.
