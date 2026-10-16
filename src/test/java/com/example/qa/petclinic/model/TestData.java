package com.example.qa.petclinic.model;
public final class TestData {
    private TestData() {
    }

    public static OwnerFields owner() {
        return new OwnerFields("Mikhail", "Ivanov",
                "ul. Lenina, 12", "Samara", "9001234567");
    }


    public static OwnerFields anotherOwner() {
        return new OwnerFields("Aleksandr", "Petrov",
                "ul. Pushkina, 34", "Kazan", "9107654321");
    }
}
