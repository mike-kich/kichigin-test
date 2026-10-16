package com.example.qa.petclinic.model;

public record OwnerFields(String firstName, String lastName, String address,
                          String city, String telephone) {

    public OwnerFields withFirstName(String value) {
        return new OwnerFields(value, lastName, address, city, telephone);
    }

    public OwnerFields withLastName(String value) {
        return new OwnerFields(firstName, value, address, city, telephone);
    }

    public OwnerFields withAddress(String value) {
        return new OwnerFields(firstName, lastName, value, city, telephone);
    }

    public OwnerFields withCity(String value) {
        return new OwnerFields(firstName, lastName, address, value, telephone);
    }

    public OwnerFields withTelephone(String value) {
        return new OwnerFields(firstName, lastName, address, city, value);
    }
}
