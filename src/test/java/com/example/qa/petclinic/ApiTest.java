package com.example.qa.petclinic;

import com.example.qa.petclinic.config.ApiTestConfiguration;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = ApiTestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
abstract class ApiTest {
    @Autowired
    private RequestSpecification specification;

    protected RequestSpecification request() {
        return RestAssured.given().spec(specification);
    }

    protected RequestSpecification jsonRequest(Object body) {
        return request().contentType(ContentType.JSON).body(body);
    }
}
